package st.orm.demo.imdb.model;

import java.time.Instant;
import java.util.List;
import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.Json;
import st.orm.PK;
import st.orm.Ref;

/**
 * The photo gallery of a person, fetched from Wikimedia on first view and
 * stored for every request after that. A dependent one-to-one like Rating:
 * the primary key is the foreign key to the person. The photos live in a
 * single JSON column — a gallery is opaque, always read whole and never
 * filtered by element, so a separate photo table would buy nothing.
 *
 * <p>The key is a Ref: whoever asks for a gallery already holds the person, and
 * nothing reads the person back off it, so reading a gallery touches the
 * gallery table alone. Queries can still reach the person through the key.
 */
public record PersonGallery(
        @PK(generation = GenerationStrategy.NONE) @FK Ref<Person> person,
        @Json List<Photo> photos,
        Instant fetchedAt
) implements Entity<Ref<Person>> {
}
