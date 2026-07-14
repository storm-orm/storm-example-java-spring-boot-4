package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import org.springframework.stereotype.Service;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieSummary_;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.PersonSummary_;
import st.orm.demo.imdb.repository.MovieSummaryRepository;
import st.orm.demo.imdb.repository.PersonSummaryRepository;

@Service
public class SearchService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    private static final int MOVIE_PAGE_SIZE = 18;
    private static final int PERSON_PAGE_SIZE = 12;
    private static final int MOVIE_SUGGESTION_LIMIT = 6;
    private static final int PERSON_SUGGESTION_LIMIT = 4;

    private final MovieSummaryRepository movieSummaryRepository;
    private final PersonSummaryRepository personSummaryRepository;

    public SearchService(MovieSummaryRepository movieSummaryRepository,
                         PersonSummaryRepository personSummaryRepository) {
        this.movieSummaryRepository = movieSummaryRepository;
        this.personSummaryRepository = personSummaryRepository;
    }

    /** The search page: both first windows in one read-only transaction. */
    public SearchResults search(String query) {
        return transaction(READ_ONLY, tx -> {
            return new SearchResults(
                    movieSummaryRepository.searchByTitle(query, Scrollable.of(MovieSummary_.id, MOVIE_PAGE_SIZE)),
                    personSummaryRepository.searchByName(query, Scrollable.of(PersonSummary_.id, PERSON_PAGE_SIZE))
            );
            });
    }

    /**
     * The next window of movie results. The cursor is the opaque string
     * from the previous response: null requests the first window, any other
     * value resumes exactly where the client left off. Purely server-side
     * navigation would use window.next() instead — no cursor involved.
     */
    public Window<MovieSummary> scrollMovies(String query, String cursor) {
        Scrollable<MovieSummary> scrollable = cursor != null
                ? Scrollable.fromCursor(MovieSummary_.id, cursor)
                : Scrollable.of(MovieSummary_.id, MOVIE_PAGE_SIZE);
        return movieSummaryRepository.searchByTitle(query, scrollable);
    }

    /** The next window of person results — same cursor contract as {@link #scrollMovies}. */
    public Window<PersonSummary> scrollPersons(String query, String cursor) {
        Scrollable<PersonSummary> scrollable = cursor != null
                ? Scrollable.fromCursor(PersonSummary_.id, cursor)
                : Scrollable.of(PersonSummary_.id, PERSON_PAGE_SIZE);
        return personSummaryRepository.searchByName(query, scrollable);
    }

    /** Auto-complete: movie and person suggestions in one read-only transaction. */
    public Suggestions findSuggestions(String query) {
        return transaction(READ_ONLY, tx -> {
            return new Suggestions(
                    movieSummaryRepository.findTitleSuggestions(query, MOVIE_SUGGESTION_LIMIT),
                    personSummaryRepository.findNameSuggestions(query, PERSON_SUGGESTION_LIMIT)
            );
            });
    }
}
