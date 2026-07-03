package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;
import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.PK;
import st.orm.Persist;

/**
 * A cast or crew credit linking a person to a movie. The IMDB billing order
 * (ordering) is unique per movie and forms the composite primary key together
 * with the movie. The person FK is not part of the key and remains insertable.
 *
 * <p>The billing order is also exposed as a read-only field mapped to the same
 * column as the PK component, giving queries a type-safe ordering criterion.
 */
public record Principal(
        @PK(generation = GenerationStrategy.NONE) PrincipalPk id,
        @Nonnull @FK @Persist(insertable = false, updatable = false) Movie movie,
        @Nonnull @FK Person person,
        @Persist(insertable = false, updatable = false) int ordering,
        @Nonnull String category,
        String characters
) implements Entity<PrincipalPk> {

    public Principal(Movie movie, int ordering, Person person, String category, String characters) {
        this(new PrincipalPk(movie.id(), ordering), movie, person, ordering, category, characters);
    }
}
