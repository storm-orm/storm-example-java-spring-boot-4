package st.orm.demo.imdb.model;

import org.jspecify.annotations.Nullable;

/** A single photo in a person's gallery. */
public record Photo(
        String url,
        @Nullable String caption
) {

    public Photo(String url) {
        this(url, null);
    }
}
