package st.orm.demo.imdb.model;

import org.jspecify.annotations.Nullable;
import st.orm.DbTable;
import st.orm.GenerationStrategy;
import st.orm.PK;
import st.orm.Projection;

/**
 * A read-only projection of the movie table for list pages: search results,
 * browse grids, and auto-complete suggestions only need the id (for the
 * poster endpoint and links), the title, and the year — not the full entity.
 */
@DbTable("movie")
public record MovieSummary(
        @PK(generation = GenerationStrategy.NONE) String id,
        String primaryTitle,
        @Nullable Integer startYear
) implements Projection<String> {
}
