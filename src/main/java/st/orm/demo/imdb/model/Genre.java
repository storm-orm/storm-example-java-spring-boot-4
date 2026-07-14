package st.orm.demo.imdb.model;

import st.orm.Entity;
import st.orm.PK;
import st.orm.UK;

/**
 * A film genre, normalized from the comma-separated genre list in the
 * IMDB dataset. An immutable record so it can round-trip through the
 * statistics cache — immutable records cache safely.
 */
public record Genre(
        @PK Integer id,
        @UK String name
) implements Entity<Integer> {

    public Genre(String name) {
        this(0, name);
    }
}
