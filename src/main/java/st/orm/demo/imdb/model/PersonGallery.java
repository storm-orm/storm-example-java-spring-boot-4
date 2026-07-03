package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.Json;
import st.orm.MetamodelType;
import st.orm.PK;

/**
 * The photo gallery of a person, fetched from Wikimedia on first view and
 * stored for every request after that. A dependent one-to-one like Rating:
 * the primary key is the foreign key to the person. The photos live in a
 * single JSON column — a gallery is opaque, always read whole and never
 * filtered by element, so a separate photo table would buy nothing.
 */
public record PersonGallery(
        @PK(generation = GenerationStrategy.NONE) @FK Person person,
        @Nonnull @MetamodelType(Object.class) @Json List<Photo> photos,
        @Nonnull Instant fetchedAt
) implements Entity<Person> {
}
