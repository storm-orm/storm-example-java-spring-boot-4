package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import org.springframework.stereotype.Service;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Person_;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.PersonRepository;

@Service
public class SearchService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    private static final int MOVIE_PAGE_SIZE = 18;
    private static final int PERSON_PAGE_SIZE = 12;
    private static final int MOVIE_SUGGESTION_LIMIT = 6;
    private static final int PERSON_SUGGESTION_LIMIT = 4;

    private final MovieRepository movieRepository;
    private final PersonRepository personRepository;

    public SearchService(MovieRepository movieRepository,
                         PersonRepository personRepository) {
        this.movieRepository = movieRepository;
        this.personRepository = personRepository;
    }

    /** The search page: both first windows in one read-only transaction. */
    public SearchResults search(String query) {
        return transaction(READ_ONLY, tx -> {
            return new SearchResults(
                    movieRepository.searchMovies(query, MovieRepository.mostVotedFirst(MOVIE_PAGE_SIZE)),
                    personRepository.searchByName(query, Scrollable.of(Person_.id, PERSON_PAGE_SIZE))
            );
            });
    }

    /**
     * The next window of movie results. The cursor is the opaque string
     * from the previous response: null requests the first window, any other
     * value resumes exactly where the client left off. Purely server-side
     * navigation would use window.next() instead — no cursor involved.
     */
    public Window<Movie> scrollMovies(String query, String cursor) {
        Scrollable<Movie> scrollable = cursor != null
                ? MovieRepository.mostVotedFirst(MOVIE_PAGE_SIZE).from(cursor)
                : MovieRepository.mostVotedFirst(MOVIE_PAGE_SIZE);
        return movieRepository.searchMovies(query, scrollable);
    }

    /** The next window of person results — same cursor contract as {@link #scrollMovies}. */
    public Window<Person> scrollPersons(String query, String cursor) {
        Scrollable<Person> scrollable = cursor != null
                ? Scrollable.of(Person_.id, PERSON_PAGE_SIZE).from(cursor)
                : Scrollable.of(Person_.id, PERSON_PAGE_SIZE);
        return personRepository.searchByName(query, scrollable);
    }

    /** Auto-complete: movie and person suggestions in one read-only transaction. */
    public Suggestions findSuggestions(String query) {
        return transaction(READ_ONLY, tx -> {
            return new Suggestions(
                    movieRepository.findMovieSuggestions(query, MOVIE_SUGGESTION_LIMIT),
                    personRepository.findNameSuggestions(query, PERSON_SUGGESTION_LIMIT)
            );
            });
    }
}
