package st.orm.demo.imdb.model;

import jakarta.annotation.Nonnull;

public record PrincipalPk(
        @Nonnull String movieId,
        int ordering
) {
}
