package st.orm.demo.imdb.model;

import org.jspecify.annotations.Nullable;
import st.orm.Entity;
import st.orm.GenerationStrategy;
import st.orm.PK;

/**
 * An actor, director, or other credited person from the IMDB dataset,
 * keyed by the natural IMDB identifier (nconst, e.g. "nm0000206").
 * An immutable record so it can round-trip through the statistics cache.
 */
public record Person(
        @PK(generation = GenerationStrategy.NONE) String id,
        String primaryName,
        @Nullable Integer birthYear,
        @Nullable Integer deathYear
) implements Entity<String> {
}
