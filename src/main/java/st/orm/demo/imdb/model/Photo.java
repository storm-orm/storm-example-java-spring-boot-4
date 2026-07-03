package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;

/** A single photo in a person's gallery. */
public record Photo(
        @Nonnull String url,
        String caption
) {

    public Photo(String url) {
        this(url, null);
    }
}
