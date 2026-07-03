package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;

public record MovieGenrePk(
        @Nonnull String movieId,
        int genreId
) {
}
