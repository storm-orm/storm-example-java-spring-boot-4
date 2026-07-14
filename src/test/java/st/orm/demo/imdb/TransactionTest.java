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
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;
import st.orm.spring.SpringConnectionProvider;
import st.orm.spring.SpringTransactionTemplateProvider;
import st.orm.template.ORMTemplate;
import st.orm.template.Transactions;
import st.orm.test.StormTest;

/**
 * Verifies the semantics of Spring-managed transactions used across the
 * application: the transaction is the boundary — an exception rolls back
 * every write, and setRollbackOnly() discards writes without one.
 *
 * <p>Since Storm 1.13, joining Spring transactions is explicit composition
 * rather than classpath detection: the template is built with the Spring
 * connection and transaction providers, exactly as the Spring Boot starter
 * configures the application's own template.</p>
 */
@StormTest(scripts = {"/schema.sql", "/data.sql"})
class TransactionTest {

    private static ORMTemplate springOrm(DataSource dataSource, PlatformTransactionManager transactionManager) {
        return ORMTemplate.builder(dataSource)
                .connectionProvider(new SpringConnectionProvider())
                .transactionTemplateProvider(new SpringTransactionTemplateProvider(List.of(transactionManager)))
                .build();
    }

    @Test
    void anExceptionInsideTheTransactionRollsBackAllWrites(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ORMTemplate orm = springOrm(dataSource, transactionManager);
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
    void setRollbackOnlyDiscardsTheWritesWithoutAnException(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ORMTemplate orm = springOrm(dataSource, transactionManager);
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

    @Test
    void stormProgrammaticTransactionRunsThroughSpringsManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        ORMTemplate orm = springOrm(dataSource, transactionManager);
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        Movie godfather = movieRepository.getById("tt0068646");

        Transactions.transaction(tx -> {
            watchlistRepository.insert(new Watchlist(godfather, Instant.now()));
            assertTrue(watchlistRepository.existsById(godfather));
            tx.setRollbackOnly();
            return null;
        });
        // The programmatic transaction ran through Spring's DataSourceTransactionManager and rolled back.
        assertFalse(watchlistRepository.existsById(godfather));
    }

    @Test
    void stormProgrammaticJoinsSpringDeclarativeTransaction(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ORMTemplate orm = springOrm(dataSource, transactionManager);
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        WatchlistRepository watchlistRepository = orm.repository(WatchlistRepository.class);
        Movie pulpFiction = movieRepository.getById("tt0110912");

        transaction.executeWithoutResult(status -> {
            // A Storm programmatic block inside a Spring-managed transaction joins it (REQUIRED).
            Transactions.transaction(tx -> {
                watchlistRepository.insert(new Watchlist(pulpFiction, Instant.now()));
                return null;
            });
            assertTrue(watchlistRepository.existsById(pulpFiction));
            status.setRollbackOnly();
        });
        // The Spring rollback discarded the write made by the joined Storm block.
        assertFalse(watchlistRepository.existsById(pulpFiction));
    }
}
