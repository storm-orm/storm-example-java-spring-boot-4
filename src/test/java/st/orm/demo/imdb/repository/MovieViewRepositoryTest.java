package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Ref;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieView;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieViewRepositoryTest {

    @Test
    void findRecentViewsStaysOnTheViewTableThanksToRef(ORMTemplate orm, SqlCapture capture) {
        MovieViewRepository movieViewRepository = orm.repository(MovieViewRepository.class);
        List<MovieView> recentViews = capture.execute(() -> movieViewRepository.findRecentViews(10));
        TestSupport.printStatements(capture, "findRecentViews");
        assertEquals(1, capture.count(Operation.SELECT));
        // The movie field is a Ref, so no join on the movie table is needed.
        assertFalse(capture.statements().get(0).statement().toLowerCase().contains("join"));
        assertTrue(recentViews.size() >= 3);
        assertEquals("tt0133093", recentViews.get(0).movie().id());
    }

    @Test
    void recordingAViewInsertsByIdWithoutLoadingTheMovie(ORMTemplate orm, SqlCapture capture) {
        MovieViewRepository movieViewRepository = orm.repository(MovieViewRepository.class);
        capture.run(() ->
                movieViewRepository.insert(
                        // Older than the seeded views so it never becomes the newest.
                        new MovieView(0L, Ref.of(Movie.class, "tt0110912"), Instant.parse("2026-06-30T00:00:00Z"))));
        TestSupport.printStatements(capture, "recordView");
        // A single INSERT: no SELECT is needed to record the view.
        assertEquals(1, capture.count(Operation.INSERT));
        assertEquals(0, capture.count(Operation.SELECT));
    }
}
