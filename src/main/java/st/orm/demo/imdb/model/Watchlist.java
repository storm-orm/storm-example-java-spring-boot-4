package st.orm.demo.imdb.model;

import java.time.Instant;
import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.PK;

/**
 * A movie saved to the personal watchlist. The primary key is the foreign
 * key to the movie, which makes saving the same movie twice a key violation —
 * exactly the semantics a watchlist toggle needs.
 */
public record Watchlist(
        @PK(generation = GenerationStrategy.NONE) @FK Movie movie,
        Instant addedAt
) implements Entity<Movie> {
}
