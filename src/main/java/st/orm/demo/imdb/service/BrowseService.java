package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import org.springframework.stereotype.Service;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.MovieGenre_;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieSummary_;
import st.orm.demo.imdb.repository.GenreRepository;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieSummaryRepository;

@Service
public class BrowseService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    private static final int PAGE_SIZE = 24;

    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieSummaryRepository movieSummaryRepository;

    public BrowseService(GenreRepository genreRepository,
                         MovieGenreRepository movieGenreRepository,
                         MovieSummaryRepository movieSummaryRepository) {
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieSummaryRepository = movieSummaryRepository;
    }

    /** The browse page: genre, count, and first window in one read-only transaction. */
    public BrowseView browseGenre(String genreName) {
        return transaction(READ_ONLY, tx -> {
            Genre genre = genreRepository.findByName(genreName).orElse(null);
            if (genre == null) {
                return null;
            }
            return new BrowseView(
                    genre,
                    movieGenreRepository.countBy(MovieGenre_.genre, genre),
                    movieSummaryRepository.scrollByGenre(genre, Scrollable.of(MovieSummary_.id, PAGE_SIZE))
            );
            });
    }

    /**
     * The next keyset window of a genre, or null for an unknown genre. The
     * cursor is the opaque string from the previous response (null requests
     * the first window) — the client echoes it back unchanged.
     */
    public Window<MovieSummary> scrollGenre(String genreName, String cursor) {
        return transaction(READ_ONLY, tx -> {
            Genre genre = genreRepository.findByName(genreName).orElse(null);
            if (genre == null) {
                return null;
            }
            Scrollable<MovieSummary> scrollable = cursor != null
                    ? Scrollable.fromCursor(MovieSummary_.id, cursor)
                    : Scrollable.of(MovieSummary_.id, PAGE_SIZE);
            return movieSummaryRepository.scrollByGenre(genre, scrollable);
            });
    }
}
