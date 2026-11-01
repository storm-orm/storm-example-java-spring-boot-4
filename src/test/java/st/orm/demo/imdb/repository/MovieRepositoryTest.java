package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Movie_;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieRepositoryTest {

    @Test
    void countMoviesPerDecadeGroupsByTheComputedDecade(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        List<DecadeMovieCount> decades = capture.execute(movieRepository::countMoviesPerDecade);
        TestSupport.printStatements(capture, "countMoviesPerDecade");
        assertTrue(capture.statements().get(0).statement().contains("GROUP BY"));
        assertEquals(
                List.of(Map.entry(1970, 1L), Map.entry(1990, 3L), Map.entry(2000, 2L)),
                decades.stream().map(decade -> Map.entry(decade.decade(), decade.movieCount())).toList()
        );
    }


    @Test
    void searchMoviesReadsTheMatchingMoviesInOneStatement(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        Window<Movie> window = capture.execute(() ->
                movieRepository.searchMovies("matrix", Scrollable.of(Movie_.id, 10)));
        TestSupport.printStatements(capture, "searchMovies");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(
                List.of("The Matrix", "The Matrix Reloaded"),
                window.content().stream().map(Movie::primaryTitle).sorted().toList()
        );
    }

    @Test
    void searchMoviesMatchesEveryWordOfTheQueryInAnyOrder(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        assertEquals(List.of("The Matrix Reloaded"), titles(movieRepository, "reloaded MATRIX"));
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieRepository, "\"the matrix\""));
        assertEquals(List.of(), titles(movieRepository, "matrix godfather"));
        // % and _ match themselves rather than acting as wildcards.
        assertEquals(List.of(), titles(movieRepository, "%"));
    }

    @Test
    void searchMoviesMatchesTheOriginalTitle(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        assertEquals(List.of("Amélie"), titles(movieRepository, "fabuleux destin"));
        assertEquals(List.of("Amélie"), titles(movieRepository, "amélie poulain"));
        assertEquals(List.of("Amélie"), titles(movieRepository, "amelie poulain"));
        assertEquals(List.of("Amélie"), titles(movieRepository, "AMELIE"));
        assertEquals(List.of(), titles(movieRepository, "fabuleux matrix"));
    }

    @Test
    void searchMoviesMatchesTheNamesOfThePeopleCredited(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieRepository, "keanu"));
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieRepository, "matrix keanu"));
        assertEquals(List.of("The Matrix Reloaded"), titles(movieRepository, "reloaded fishburne"));
        assertEquals(List.of("The Shawshank Redemption"), titles(movieRepository, "freeman"));
        assertEquals(List.of(), titles(movieRepository, "keanu godfather"));
    }

    private static List<String> titles(MovieRepository movieRepository, String query) {
        return movieRepository.searchMovies(query, Scrollable.of(Movie_.id, 10))
                .content().stream().map(Movie::primaryTitle).sorted().toList();
    }

    @Test
    void searchMoviesScrollsThroughWindowsWithACursor(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);

        Window<Movie> firstWindow = movieRepository.searchMovies("matrix", Scrollable.of(Movie_.id, 1));
        assertEquals(1, firstWindow.content().size());
        assertTrue(firstWindow.hasNext());
        String cursor = firstWindow.nextCursor();
        assertNotNull(cursor);

        Window<Movie> secondWindow = capture.execute(() ->
                movieRepository.searchMovies("matrix", Scrollable.of(Movie_.id, 1).from(cursor)));
        TestSupport.printStatements(capture, "searchMovies-cursor");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void scrollingServerSideUsesWindowNextWithoutAnyCursor(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);

        Window<Movie> firstWindow = movieRepository.searchMovies("matrix", Scrollable.of(Movie_.id, 1));
        assertTrue(firstWindow.hasNext());

        // Server-side navigation never touches the cursor string: next() is
        // a ready-to-use typed Scrollable, ordering and size included. The
        // cursor carries the position alone, for crossing the client-server
        // boundary where the request states the rest.
        Window<Movie> secondWindow = capture.execute(() ->
                movieRepository.searchMovies("matrix", firstWindow.next()));
        TestSupport.printStatements(capture, "searchMovies-next");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void findMovieSuggestionsRanksByVoteCount(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        List<Movie> suggestions = capture.execute(() ->
                movieRepository.findMovieSuggestions("matrix", 5));
        TestSupport.printStatements(capture, "findMovieSuggestions");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("rating"));
        // The Matrix (2M votes) must rank above The Matrix Reloaded (600k).
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"),
                suggestions.stream().map(Movie::primaryTitle).toList());
    }

    @Test
    void findMovieSuggestionsIgnoresTheNoiseWordsOfTitles(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        // "the" is in five of the six titles; alone it suggests nothing.
        assertEquals(List.of(), suggestedTitles(movieRepository, "the"));
        assertEquals(List.of(), suggestedTitles(movieRepository, "THE"));
        // Next to another word it takes no part: "the god" suggests what "god" does.
        assertEquals(List.of("The Godfather"), suggestedTitles(movieRepository, "god"));
        assertEquals(List.of("The Godfather"), suggestedTitles(movieRepository, "the god"));
    }

    @Test
    void findMovieSuggestionsMatchesATwoLetterWordAtTheStartOfAWord(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        assertEquals(List.of("The Godfather"), suggestedTitles(movieRepository, "go"));
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), suggestedTitles(movieRepository, "ma"));
        // Inside a word it does not match: "at" is in Matrix, but starts no word.
        assertEquals(List.of(), suggestedTitles(movieRepository, "at"));
        // A word of one letter takes no part, so it suggests nothing.
        assertEquals(List.of(), suggestedTitles(movieRepository, "m"));
        assertEquals(List.of(), suggestedTitles(movieRepository, "  "));
    }

    @Test
    void findMovieSuggestionsIgnoresAccents(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        assertEquals(List.of("Amélie"), suggestedTitles(movieRepository, "amelie"));
        assertEquals(List.of("Amélie"), suggestedTitles(movieRepository, "AMÉLIE"));
        assertEquals(List.of("Amélie"), suggestedTitles(movieRepository, "fabuleux"));
    }

    private static List<String> suggestedTitles(MovieRepository movieRepository, String query) {
        return movieRepository.findMovieSuggestions(query, 5)
                .stream().map(Movie::primaryTitle).toList();
    }

    @Test
    void scrollByGenreScrollsAGenreMostVotedFirst(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        var drama = genreRepository.findByName("Drama").orElseThrow();

        capture.clear();
        Window<Movie> firstWindow = capture.execute(() ->
                movieRepository.scrollByGenre(drama, MovieRepository.mostVotedFirst(2)));
        TestSupport.printStatements(capture, "scrollByGenre");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("movie_genre"));
        // Shawshank (2.9M votes) and Pulp Fiction (2.2M) before The Godfather
        // (2.0M), the reverse of their ids.
        assertEquals(List.of("The Shawshank Redemption", "Pulp Fiction"),
                firstWindow.content().stream().map(Movie::primaryTitle).toList());
        assertTrue(firstWindow.hasNext());

        Window<Movie> secondWindow = movieRepository.scrollByGenre(
                drama, MovieRepository.mostVotedFirst(2).from(firstWindow.nextCursor()));
        assertEquals(List.of("The Godfather"),
                secondWindow.content().stream().map(Movie::primaryTitle).toList());
    }
}
