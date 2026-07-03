package st.orm.demo.imdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.WatchlistRepository;
import st.orm.template.ORMTemplate;
import st.orm.test.StormTest;

/**
 * Verifies the semantics of Spring-managed transactions used across the
 * application: the transaction is the boundary — an exception rolls back
 * every write, and setRollbackOnly() discards writes without one. Storm's
 * repository operations join the active Spring transaction.
 */
@StormTest(scripts = {"/schema.sql", "/data.sql"})
class TransactionTest {

    @Test
    void anExceptionInsideTheTransactionRollsBackAllWrites(ORMTemplate orm, DataSource dataSource) {
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        Movie godfather = movieRepository.getById("tt0068646");

        assertThrows(IllegalStateException.class, () ->
                transaction.executeWithoutResult(status -> {
                    watchlistRepository.insert(new Watchlist(godfather, Instant.now()));
                    throw new IllegalStateException("Simulated failure after the insert");
                }));
        assertFalse(watchlistRepository.existsById(godfather));
    }

    @Test
    void setRollbackOnlyDiscardsTheWritesWithoutAnException(ORMTemplate orm, DataSource dataSource) {
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        Movie pulpFiction = movieRepository.getById("tt0110912");

        transaction.executeWithoutResult(status -> {
            watchlistRepository.insert(new Watchlist(pulpFiction, Instant.now()));
            // Inside the transaction the write is visible...
            assertTrue(watchlistRepository.existsById(pulpFiction));
            status.setRollbackOnly();
        });
        // ...and gone after the rollback.
        assertFalse(watchlistRepository.existsById(pulpFiction));
    }
}
