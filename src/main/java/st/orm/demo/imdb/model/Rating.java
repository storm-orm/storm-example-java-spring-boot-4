package st.orm.demo.imdb.model;

import java.math.BigDecimal;
import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.PK;

/**
 * The IMDB rating for a movie. A dependent one-to-one: the primary key is
 * the foreign key to the movie, so the entity's ID type is Movie itself.
 */
public record Rating(
        @PK(generation = GenerationStrategy.NONE) @FK Movie movie,
        BigDecimal averageRating,
        int voteCount
) implements Entity<Movie> {
}
