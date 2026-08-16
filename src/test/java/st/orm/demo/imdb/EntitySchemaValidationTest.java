package st.orm.demo.imdb;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static st.orm.test.TestDatabase.POSTGRESQL;

import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Data;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.PersonGallery;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.template.ORMTemplate;
import st.orm.test.StormTest;

/**
 * Validates every entity against the database schema at the JDBC level:
 * column presence, type compatibility, nullability, primary keys, and
 * foreign key consistency. Unlike the other tests, which run on H2, this
 * one runs on PostgreSQL in a Testcontainers-managed container and applies
 * the Flyway migration itself, so the entities are checked against the
 * schema the application deploys with, on the dialect it deploys on. The
 * container starts once per test run; the class receives a database of its
 * own inside it.
 */
@StormTest(database = POSTGRESQL, scripts = {"/db/migration/V1__create_schema.sql"})
class EntitySchemaValidationTest {

    @Test
    void entitiesMatchTheDatabaseSchema(ORMTemplate orm) {
        List<Class<? extends Data>> types = List.of(
                Movie.class,
                Genre.class,
                MovieGenre.class,
                Person.class,
                Principal.class,
                Rating.class,
                MovieView.class,
                Watchlist.class,
                PersonGallery.class,
                MovieSummary.class,
                PersonSummary.class
        );
        List<String> errors = orm.validateSchema(types);
        assertTrue(errors.isEmpty(), () -> "Schema validation errors: " + errors);
    }
}
