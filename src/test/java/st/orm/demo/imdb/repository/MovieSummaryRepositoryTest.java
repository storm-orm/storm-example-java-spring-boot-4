package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieSummary_;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieSummaryRepositoryTest {

    @Test
    void searchMoviesSelectsOnlyTheProjectedColumns(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        Window<MovieSummary> window = capture.execute(() ->
                movieSummaryRepository.searchMovies("matrix", Scrollable.of(MovieSummary_.id, 10)));
        TestSupport.printStatements(capture, "searchMovies");
        assertEquals(1, capture.count(Operation.SELECT));
        String statement = capture.statements().get(0).statement();
        // The projection narrows the SELECT: no columns beyond id, titles, year.
        assertFalse(statement.contains("runtime_minutes"));
        assertEquals(
                List.of("The Matrix", "The Matrix Reloaded"),
                window.content().stream().map(MovieSummary::primaryTitle).sorted().toList()
        );
    }

    @Test
    void searchMoviesMatchesEveryWordOfTheQueryInAnyOrder(ORMTemplate orm) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        assertEquals(List.of("The Matrix Reloaded"), titles(movieSummaryRepository, "reloaded MATRIX"));
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieSummaryRepository, "\"the matrix\""));
        assertEquals(List.of(), titles(movieSummaryRepository, "matrix godfather"));
        // % and _ match themselves rather than acting as wildcards.
        assertEquals(List.of(), titles(movieSummaryRepository, "%"));
    }

    @Test
    void searchMoviesMatchesTheOriginalTitle(ORMTemplate orm) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        assertEquals(List.of("Amélie"), titles(movieSummaryRepository, "fabuleux destin"));
        assertEquals(List.of("Amélie"), titles(movieSummaryRepository, "amélie poulain"));
        assertEquals(List.of("Amélie"), titles(movieSummaryRepository, "amelie poulain"));
        assertEquals(List.of("Amélie"), titles(movieSummaryRepository, "AMELIE"));
        assertEquals(List.of(), titles(movieSummaryRepository, "fabuleux matrix"));
    }

    @Test
    void searchMoviesMatchesTheNamesOfThePeopleCredited(ORMTemplate orm) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieSummaryRepository, "keanu"));
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"), titles(movieSummaryRepository, "matrix keanu"));
        assertEquals(List.of("The Matrix Reloaded"), titles(movieSummaryRepository, "reloaded fishburne"));
        assertEquals(List.of("The Shawshank Redemption"), titles(movieSummaryRepository, "freeman"));
        assertEquals(List.of(), titles(movieSummaryRepository, "keanu godfather"));
    }

    private static List<String> titles(MovieSummaryRepository movieSummaryRepository, String query) {
        return movieSummaryRepository.searchMovies(query, Scrollable.of(MovieSummary_.id, 10))
                .content().stream().map(MovieSummary::primaryTitle).sorted().toList();
    }

    @Test
    void searchMoviesScrollsThroughWindowsWithACursor(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);

        Window<MovieSummary> firstWindow = movieSummaryRepository.searchMovies("matrix", Scrollable.of(MovieSummary_.id, 1));
        assertEquals(1, firstWindow.content().size());
        assertTrue(firstWindow.hasNext());
        String cursor = firstWindow.nextCursor();
        assertNotNull(cursor);

        Window<MovieSummary> secondWindow = capture.execute(() ->
                movieSummaryRepository.searchMovies("matrix", Scrollable.of(MovieSummary_.id, 1).from(cursor)));
        TestSupport.printStatements(capture, "searchMovies-cursor");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void scrollingServerSideUsesWindowNextWithoutAnyCursor(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);

        Window<MovieSummary> firstWindow = movieSummaryRepository.searchMovies("matrix", Scrollable.of(MovieSummary_.id, 1));
        assertTrue(firstWindow.hasNext());

        // Server-side navigation never touches the cursor string: next() is
        // a ready-to-use typed Scrollable, ordering and size included. The
        // cursor carries the position alone, for crossing the client-server
        // boundary where the request states the rest.
        Window<MovieSummary> secondWindow = capture.execute(() ->
                movieSummaryRepository.searchMovies("matrix", firstWindow.next()));
        TestSupport.printStatements(capture, "searchMovies-next");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void findMovieSuggestionsRanksByVoteCount(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        List<MovieSummary> suggestions = capture.execute(() ->
                movieSummaryRepository.findMovieSuggestions("matrix", 5));
        TestSupport.printStatements(capture, "findMovieSuggestions");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("rating"));
        // The Matrix (2M votes) must rank above The Matrix Reloaded (600k).
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"),
                suggestions.stream().map(MovieSummary::primaryTitle).toList());
    }

    @Test
    void scrollByGenreScrollsAGenreMostVotedFirst(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        var drama = genreRepository.findByName("Drama").orElseThrow();

        capture.clear();
        Window<MovieSummary> firstWindow = capture.execute(() ->
                movieSummaryRepository.scrollByGenre(drama, MovieSummaryRepository.mostVotedFirst(2)));
        TestSupport.printStatements(capture, "scrollByGenre");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("movie_genre"));
        // Shawshank (2.9M votes) and Pulp Fiction (2.2M) before The Godfather
        // (2.0M), the reverse of their ids.
        assertEquals(List.of("The Shawshank Redemption", "Pulp Fiction"),
                firstWindow.content().stream().map(MovieSummary::primaryTitle).toList());
        assertTrue(firstWindow.hasNext());

        Window<MovieSummary> secondWindow = movieSummaryRepository.scrollByGenre(
                drama, MovieSummaryRepository.mostVotedFirst(2).from(firstWindow.nextCursor()));
        assertEquals(List.of("The Godfather"),
                secondWindow.content().stream().map(MovieSummary::primaryTitle).toList());
    }
}
