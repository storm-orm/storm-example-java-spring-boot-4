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
    void searchByTitleSelectsOnlyTheProjectedColumns(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        Window<MovieSummary> window = capture.execute(() ->
                movieSummaryRepository.searchByTitle("matrix", Scrollable.of(MovieSummary_.id, 10)));
        TestSupport.printStatements(capture, "searchByTitle");
        assertEquals(1, capture.count(Operation.SELECT));
        String statement = capture.statements().get(0).statement();
        // The projection narrows the SELECT: no columns beyond id, title, year.
        assertFalse(statement.contains("original_title"));
        assertFalse(statement.contains("runtime_minutes"));
        assertEquals(
                List.of("The Matrix", "The Matrix Reloaded"),
                window.content().stream().map(MovieSummary::primaryTitle).sorted().toList()
        );
    }

    @Test
    void searchByTitleScrollsThroughWindowsWithACursor(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);

        Window<MovieSummary> firstWindow = movieSummaryRepository.searchByTitle("matrix", Scrollable.of(MovieSummary_.id, 1));
        assertEquals(1, firstWindow.content().size());
        assertTrue(firstWindow.hasNext());
        String cursor = firstWindow.nextCursor();
        assertNotNull(cursor);

        Window<MovieSummary> secondWindow = capture.execute(() ->
                movieSummaryRepository.searchByTitle("matrix", Scrollable.fromCursor(MovieSummary_.id, cursor)));
        TestSupport.printStatements(capture, "searchByTitle-cursor");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void scrollingServerSideUsesWindowNextWithoutAnyCursor(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);

        Window<MovieSummary> firstWindow = movieSummaryRepository.searchByTitle("matrix", Scrollable.of(MovieSummary_.id, 1));
        assertTrue(firstWindow.hasNext());

        // Server-side navigation never touches the cursor string: next() is
        // a ready-to-use typed Scrollable. The cursor is merely its
        // serialized form for crossing the client-server boundary.
        Window<MovieSummary> secondWindow = capture.execute(() ->
                movieSummaryRepository.searchByTitle("matrix", firstWindow.next()));
        TestSupport.printStatements(capture, "searchByTitle-next");
        assertEquals(1, secondWindow.content().size());
        assertFalse(firstWindow.content().get(0).id().equals(secondWindow.content().get(0).id()));
    }

    @Test
    void findTitleSuggestionsRanksByVoteCount(ORMTemplate orm, SqlCapture capture) {
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        List<MovieSummary> suggestions = capture.execute(() ->
                movieSummaryRepository.findTitleSuggestions("matrix", 5));
        TestSupport.printStatements(capture, "findTitleSuggestions");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("rating"));
        // The Matrix (2M votes) must rank above The Matrix Reloaded (600k).
        assertEquals(List.of("The Matrix", "The Matrix Reloaded"),
                suggestions.stream().map(MovieSummary::primaryTitle).toList());
    }

    @Test
    void scrollByGenreJoinsTheJunctionTableAndScrollsOnTheMovieKey(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        MovieSummaryRepository movieSummaryRepository = orm.repository(MovieSummaryRepository.class);
        var drama = genreRepository.findByName("Drama").orElseThrow();

        capture.clear();
        Window<MovieSummary> firstWindow = capture.execute(() ->
                movieSummaryRepository.scrollByGenre(drama, Scrollable.of(MovieSummary_.id, 2)));
        TestSupport.printStatements(capture, "scrollByGenre");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("movie_genre"));
        assertEquals(2, firstWindow.content().size());
        assertTrue(firstWindow.hasNext());

        Window<MovieSummary> secondWindow = movieSummaryRepository.scrollByGenre(
                drama, Scrollable.fromCursor(MovieSummary_.id, firstWindow.nextCursor()));
        // Three drama movies in total: 2 in the first window, 1 in the second.
        assertEquals(1, secondWindow.content().size());
    }
}
