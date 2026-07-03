package st.orm.demo.imdb.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.PersonRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Enriches the dataset with Wikipedia summaries: plot descriptions for
 * movies and short biographies for persons. The IMDB dataset itself carries
 * neither, so — like the poster endpoint — this is an external enrichment
 * concern, cached for the lifetime of the application and entirely optional:
 * pages render fine without it.
 */
@Service
public class WikipediaSummaryService {

    private final MovieRepository movieRepository;
    private final PersonRepository personRepository;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Optional<ExternalSummary>> summariesByKey = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public WikipediaSummaryService(MovieRepository movieRepository,
                                   PersonRepository personRepository,
                                   ObjectMapper objectMapper) {
        this.movieRepository = movieRepository;
        this.personRepository = personRepository;
        this.objectMapper = objectMapper;
    }

    public ExternalSummary findMovieSummary(String movieId) {
        return summariesByKey.computeIfAbsent("movie:" + movieId, key -> {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie == null) {
                return Optional.empty();
            }
            // Disambiguate common titles: prefer the year-qualified article.
            List<String> candidateTitles = Stream.of(
                    movie.startYear() != null ? movie.primaryTitle() + " (" + movie.startYear() + " film)" : null,
                    movie.primaryTitle() + " (film)",
                    movie.primaryTitle()
            ).filter(Objects::nonNull).toList();
            return Optional.ofNullable(findFirstSummary(candidateTitles));
        }).orElse(null);
    }

    public ExternalSummary findPersonSummary(String personId) {
        return summariesByKey.computeIfAbsent("person:" + personId, key ->
                personRepository.findById(personId)
                        .map(person -> Optional.ofNullable(findFirstSummary(
                                List.of(person.primaryName(), person.primaryName() + " (actor)"))))
                        .orElseGet(Optional::empty)
        ).orElse(null);
    }

    private ExternalSummary findFirstSummary(List<String> titles) {
        return titles.stream()
                .map(this::fetchSummary)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ExternalSummary fetchSummary(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
            URI uri = URI.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedTitle + "?redirect=true");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/json")
                    // Wikimedia's API policy requires a descriptive User-Agent.
                    .header("User-Agent", "storm-imdb-demo/1.0 (https://orm.st)")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode json = objectMapper.readTree(response.body());
            // Only standard articles qualify — skip disambiguation pages.
            if (!"standard".equals(json.path("type").asString(""))) {
                return null;
            }
            String extract = json.path("extract").asString("");
            if (extract.isBlank()) {
                return null;
            }
            return new ExternalSummary(
                    json.path("title").asString(title),
                    json.path("description").asString(null),
                    extract,
                    json.path("content_urls").path("desktop").path("page").asString(null)
            );
        } catch (Exception ignored) {
            return null;
        }
    }
}
