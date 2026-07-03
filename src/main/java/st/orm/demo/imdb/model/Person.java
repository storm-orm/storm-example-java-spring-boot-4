package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;
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
        @Nonnull String primaryName,
        Integer birthYear,
        Integer deathYear
) implements Entity<String> {
}
