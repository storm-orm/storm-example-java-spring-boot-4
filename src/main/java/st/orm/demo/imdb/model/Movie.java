package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;
import st.orm.Entity;
import st.orm.GenerationStrategy;
import st.orm.PK;

/**
 * A movie from the IMDB dataset, keyed by its natural IMDB identifier
 * (tconst, e.g. "tt0133093"). An immutable record so it can travel inside a
 * serialized Ref (see MovieView) — immutable records cache safely.
 */
public record Movie(
        @PK(generation = GenerationStrategy.NONE) String id,
        @Nonnull String primaryTitle,
        @Nonnull String originalTitle,
        Integer startYear,
        Integer runtimeMinutes
) implements Entity<String> {
}
