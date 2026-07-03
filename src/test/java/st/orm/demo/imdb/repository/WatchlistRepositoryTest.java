package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Page;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class WatchlistRepositoryTest {

    @Test
    void theToggleCycleExistsInsertExistsRemoveWorksOnTheMovieKey(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        // Pulp Fiction is not touched by other tests in this class — the
        // @StormTest database is shared across the class's test methods.
        Movie pulpFiction = movieRepository.getById("tt0110912");

        capture.run(() -> {
            assertFalse(watchlistRepository.existsById(pulpFiction));

            watchlistRepository.insert(new Watchlist(pulpFiction, Instant.now()));
            assertTrue(watchlistRepository.existsById(pulpFiction));

            watchlistRepository.removeById(pulpFiction);
            assertFalse(watchlistRepository.existsById(pulpFiction));
        });
        TestSupport.printStatements(capture, "watchlistToggle");
        assertEquals(1, capture.count(Operation.INSERT));
        assertEquals(1, capture.count(Operation.DELETE));
    }

    @Test
    void findPagePaginatesNewestFirst(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        Instant baseInstant = Instant.parse("2026-07-01T10:00:00Z");
        List<String> movieIds = List.of("tt0133093", "tt0111161", "tt0068646");
        for (int index = 0; index < movieIds.size(); index++) {
            watchlistRepository.insert(new Watchlist(
                    movieRepository.getById(movieIds.get(index)), baseInstant.plusSeconds(index)));
        }

        capture.clear();
        Page<Watchlist> page = capture.execute(() -> watchlistRepository.findPage(0, 2));
        TestSupport.printStatements(capture, "findPage");
        assertEquals(2, page.content().size());
        assertEquals(3, page.totalCount());
        assertEquals(2, page.totalPages());
        assertTrue(page.hasNext());
        // Newest first: The Godfather was added last.
        assertEquals("The Godfather", page.content().get(0).movie().primaryTitle());
    }
}
