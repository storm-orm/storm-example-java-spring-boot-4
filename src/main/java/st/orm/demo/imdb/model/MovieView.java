package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;
import java.time.Instant;
import st.orm.Entity;
import st.orm.FK;
import st.orm.PK;
import st.orm.Ref;
import st.orm.demo.imdb.serialization.InstantAsStringSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Tracks each visit to a movie detail page, backing the recently-viewed
 * section on the home page. This high-volume, append-only table is the one
 * place the model uses Ref: recording a view needs only the movie's id,
 * so there is no reason to join the full movie graph on every insert.
 *
 * <p>The Ref field demonstrates Ref serialization: the Jackson StormModule
 * handles it — an unloaded ref serializes as the raw primary key, a loaded
 * ref as the full entity. Instant needs a custom serializer (Jackson has no
 * built-in java.time support without the JavaTime module).
 */
public record MovieView(
        @PK Long id,
        @Nonnull @FK Ref<Movie> movie,
        @JsonSerialize(using = InstantAsStringSerializer.class)
        @JsonDeserialize(using = InstantAsStringSerializer.Deserializer.class)
        @Nonnull Instant viewedAt
) implements Entity<Long> {

    public MovieView(Movie movieEntity, Instant viewedAt) {
        this(0L, Ref.of(movieEntity), viewedAt);
    }
}
