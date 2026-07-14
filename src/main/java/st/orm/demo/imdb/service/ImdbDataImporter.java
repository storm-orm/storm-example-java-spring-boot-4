package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.repository.GenreRepository;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.MovieViewRepository;
import st.orm.demo.imdb.repository.PersonRepository;
import st.orm.demo.imdb.repository.PrincipalRepository;
import st.orm.demo.imdb.repository.RatingRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Imports the public IMDB dataset on first startup: movies with at least the
 * configured number of votes, their genres, cast and crew, and ratings.
 *
 * <p>The write path is stream-based: TSV rows are parsed into entities and
 * handed to Storm's batch insert, which writes fixed-size JDBC batches while
 * the file is still streaming — one pass per file, no materialized entity
 * lists. Dataset files are downloaded once and cached locally; the import is
 * skipped entirely when movie data is already present.
 */
@Component
public class ImdbDataImporter implements ApplicationRunner {

    private static final String FEATURED_MOVIE_ID = "tt0133093"; // The Matrix
    private static final String NULL_VALUE = "\\N";
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;
    private static final int INSERT_BATCH_SIZE = 1000;

    private static final Logger logger = LoggerFactory.getLogger(ImdbDataImporter.class);

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PersonRepository personRepository;
    private final PrincipalRepository principalRepository;
    private final RatingRepository ratingRepository;
    private final MovieViewRepository movieViewRepository;
    private final ObjectMapper objectMapper;
    private final ImdbImportProperties properties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ImdbDataImporter(MovieRepository movieRepository,
                            GenreRepository genreRepository,
                            MovieGenreRepository movieGenreRepository,
                            PersonRepository personRepository,
                            PrincipalRepository principalRepository,
                            RatingRepository ratingRepository,
                            MovieViewRepository movieViewRepository,
                            ObjectMapper objectMapper,
                            ImdbImportProperties properties) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.personRepository = personRepository;
        this.principalRepository = principalRepository;
        this.ratingRepository = ratingRepository;
        this.movieViewRepository = movieViewRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    private record RatingRow(BigDecimal averageRating, int voteCount) {}

    private record PrincipalRow(String movieId, int ordering, String personId, String category, String characters) {}

    private record ImportedMovies(Map<String, Movie> moviesById, Map<String, List<String>> genreNamesByMovieId) {}

    @Override
    public void run(ApplicationArguments arguments) {
        if (movieRepository.count() > 0) {
            logger.info("Movie data already present, skipping IMDB import.");
            return;
        }
        long startedAt = System.currentTimeMillis();
        logger.info("Importing IMDB dataset (movies with at least {} votes)...", properties.minimumVoteCount());

        // One Storm transaction around the whole import: a failure rolls
        // everything back, so a restart always begins from a clean database.
        // The transaction runs through Spring's transaction manager via the
        // starter's provider, exactly like @Transactional-managed work.
        transaction(tx -> {
            Map<String, RatingRow> qualifyingRatings = readQualifyingRatings();
            ImportedMovies imported = importMovies(qualifyingRatings.keySet());
            importGenres(imported.moviesById(), imported.genreNamesByMovieId());
            List<PrincipalRow> principalRows = readPrincipalRows(imported.moviesById().keySet());
            Map<String, Person> personsById = importPersons(principalRows);
            importPrincipals(principalRows, imported.moviesById(), personsById);
            importRatings(imported.moviesById(), qualifyingRatings);
            seedFeaturedMovie();
            return null;
        });

        logger.info("IMDB import finished in {} seconds.", (System.currentTimeMillis() - startedAt) / 1000);
    }

    /** Ratings of all titles (any type) that meet the vote threshold. */
    private Map<String, RatingRow> readQualifyingRatings() {
        Map<String, RatingRow> ratings = streamDataset("title.ratings.tsv.gz", lines -> {
            Map<String, RatingRow> qualifying = new HashMap<>();
            lines.forEach(line -> {
                String[] fields = line.split("\t", -1);
                int voteCount = Integer.parseInt(fields[2]);
                if (voteCount >= properties.minimumVoteCount()) {
                    qualifying.put(fields[0], new RatingRow(new BigDecimal(fields[1]), voteCount));
                }
            });
            return qualifying;
        });
        logger.info("Found {} titles with at least {} votes.", ratings.size(), properties.minimumVoteCount());
        return ratings;
    }

    /**
     * Imports qualifying movies in a single streaming pass: each parsed row
     * is emitted into the batched insert while the file streams, and is
     * remembered in the id-to-movie map for the later import steps.
     */
    private ImportedMovies importMovies(Set<String> qualifyingTitleIds) {
        return streamDataset("title.basics.tsv.gz", lines -> {
            Map<String, Movie> moviesById = new LinkedHashMap<>();
            Map<String, List<String>> genreNamesByMovieId = new HashMap<>();
            Stream<Movie> movies = lines.map(line -> {
                String titleId = substringBefore(line, '\t');
                if (!qualifyingTitleIds.contains(titleId)) {
                    return null;
                }
                String[] fields = line.split("\t", -1);
                if (!"movie".equals(fields[1])) {
                    return null;
                }
                if (!NULL_VALUE.equals(fields[8])) {
                    genreNamesByMovieId.put(titleId, List.of(fields[8].split(",")));
                }
                Movie movie = new Movie(titleId, fields[2], fields[3], parseIntOrNull(fields[5]), parseIntOrNull(fields[7]));
                moviesById.put(movie.id(), movie);
                return movie;
            }).filter(Objects::nonNull);
            movieRepository.insert(movies, INSERT_BATCH_SIZE);
            logger.info("Imported {} movies.", moviesById.size());
            return new ImportedMovies(moviesById, genreNamesByMovieId);
        });
    }

    /** Inserts the distinct genres and the movie-genre junction rows. */
    private void importGenres(Map<String, Movie> moviesById, Map<String, List<String>> genreNamesByMovieId) {
        SortedSet<String> genreNames = genreNamesByMovieId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toCollection(TreeSet::new));
        Map<String, Genre> genresByName = genreRepository
                .insertAndFetch(genreNames.stream().map(Genre::new).toList())
                .stream()
                .collect(Collectors.toMap(Genre::name, genre -> genre));
        int[] linkCount = {0};
        Stream<MovieGenre> movieGenres = genreNamesByMovieId.entrySet().stream()
                .flatMap(entry -> {
                    Movie movie = moviesById.get(entry.getKey());
                    return entry.getValue().stream().map(name -> {
                        linkCount[0]++;
                        return new MovieGenre(movie, genresByName.get(name));
                    });
                });
        movieGenreRepository.insert(movieGenres, INSERT_BATCH_SIZE);
        logger.info("Imported {} genres and {} movie-genre links.", genresByName.size(), linkCount[0]);
    }

    /**
     * Collects the cast and crew rows of the imported movies. Filtering on
     * the actually imported movie ids (not the full qualifying set) keeps
     * credits of non-movie titles out, which would violate FK constraints.
     * The rows are needed twice (person filtering, then the actual insert),
     * so this is the one place the import materializes a list.
     */
    private List<PrincipalRow> readPrincipalRows(Set<String> importedMovieIds) {
        return streamDataset("title.principals.tsv.gz", lines -> {
            List<PrincipalRow> rows = new ArrayList<>();
            lines.forEach(line -> {
                String movieId = substringBefore(line, '\t');
                if (!importedMovieIds.contains(movieId)) {
                    return;
                }
                String[] fields = line.split("\t", -1);
                rows.add(new PrincipalRow(
                        movieId,
                        Integer.parseInt(fields[1]),
                        fields[2],
                        fields[3],
                        parseCharacters(fields[5])
                ));
            });
            logger.info("Collected {} cast and crew credits.", rows.size());
            return rows;
        });
    }

    /** Imports every person referenced by the collected credits, streaming. */
    private Map<String, Person> importPersons(List<PrincipalRow> principalRows) {
        Set<String> referencedPersonIds = principalRows.stream()
                .map(PrincipalRow::personId)
                .collect(Collectors.toCollection(HashSet::new));
        return streamDataset("name.basics.tsv.gz", lines -> {
            Map<String, Person> personsById = new HashMap<>(referencedPersonIds.size() * 2);
            Stream<Person> persons = lines.map(line -> {
                String personId = substringBefore(line, '\t');
                if (!referencedPersonIds.contains(personId)) {
                    return null;
                }
                String[] fields = line.split("\t", -1);
                Person person = new Person(personId, fields[1], parseIntOrNull(fields[2]), parseIntOrNull(fields[3]));
                personsById.put(person.id(), person);
                return person;
            }).filter(Objects::nonNull);
            personRepository.insert(persons, INSERT_BATCH_SIZE);
            logger.info("Imported {} persons.", personsById.size());
            return personsById;
        });
    }

    private void importPrincipals(List<PrincipalRow> principalRows,
                                  Map<String, Movie> moviesById,
                                  Map<String, Person> personsById) {
        int[] importedCount = {0};
        Stream<Principal> principals = principalRows.stream().map(row -> {
            // A few credits reference persons missing from name.basics.
            Person person = personsById.get(row.personId());
            if (person == null) {
                return null;
            }
            importedCount[0]++;
            return new Principal(moviesById.get(row.movieId()), row.ordering(), person, row.category(), row.characters());
        }).filter(Objects::nonNull);
        principalRepository.insert(principals, INSERT_BATCH_SIZE);
        logger.info("Imported {} principals.", importedCount[0]);
    }

    private void importRatings(Map<String, Movie> moviesById, Map<String, RatingRow> qualifyingRatings) {
        Stream<Rating> ratings = moviesById.values().stream().map(movie -> {
            RatingRow ratingRow = qualifyingRatings.get(movie.id());
            return new Rating(movie, ratingRow.averageRating(), ratingRow.voteCount());
        });
        ratingRepository.insert(ratings, INSERT_BATCH_SIZE);
        logger.info("Imported {} ratings.", moviesById.size());
    }

    /**
     * Seeds a single view for The Matrix so the home page has a featured
     * movie on first launch — a plain database seed, no special-case code.
     */
    private void seedFeaturedMovie() {
        Movie theMatrix = movieRepository.findById(FEATURED_MOVIE_ID).orElse(null);
        if (theMatrix == null) {
            return;
        }
        movieViewRepository.insert(new MovieView(theMatrix, Instant.now()));
        logger.info("Seeded an initial view for '{}' as the featured movie.", theMatrix.primaryTitle());
    }

    /** The characters column holds a JSON array, e.g. ["Neo"]. */
    private String parseCharacters(String rawValue) {
        if (NULL_VALUE.equals(rawValue) || rawValue.isBlank()) {
            return null;
        }
        try {
            return String.join(", ", objectMapper.readValue(rawValue, new TypeReference<List<String>>() {}));
        } catch (JacksonException ignored) {
            return rawValue;
        }
    }

    /** Streams the data lines (header skipped) of a cached dataset file. */
    private <T> T streamDataset(String fileName, Function<Stream<String>, T> block) {
        try {
            Path file = datasetFile(fileName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(Files.newInputStream(file), STREAM_BUFFER_SIZE), StandardCharsets.UTF_8))) {
                return block.apply(reader.lines().skip(1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while preparing dataset " + fileName, e);
        }
    }

    /** Returns the cached dataset file, downloading it first if needed. */
    private Path datasetFile(String fileName) throws IOException, InterruptedException {
        Files.createDirectories(properties.cacheDirectory());
        Path file = properties.cacheDirectory().resolve(fileName);
        if (Files.notExists(file)) {
            URI uri = URI.create(properties.datasetBaseUrl() + "/" + fileName);
            logger.info("Downloading {}...", uri);
            Path temporaryFile = Files.createTempFile(properties.cacheDirectory(), fileName, ".download");
            try {
                HttpResponse<Path> response = httpClient.send(
                        HttpRequest.newBuilder(uri).build(),
                        HttpResponse.BodyHandlers.ofFile(temporaryFile));
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Download of " + uri + " failed with status " + response.statusCode());
                }
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Downloaded {} ({} MB).", fileName, Files.size(file) / (1024 * 1024));
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        }
        return file;
    }

    private static String substringBefore(String value, char delimiter) {
        int index = value.indexOf(delimiter);
        return index < 0 ? value : value.substring(0, index);
    }

    private static Integer parseIntOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
