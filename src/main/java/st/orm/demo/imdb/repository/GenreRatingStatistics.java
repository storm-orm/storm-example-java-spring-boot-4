package st.orm.demo.imdb.repository;

import java.math.BigDecimal;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.serialization.BigDecimalAsStringSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Query result shape: average rating and movie count for a genre. Not
 * backed by a database table or view, so it is a plain record —
 * deliberately not a Data type.
 */
public record GenreRatingStatistics(
        Genre genre,
        @JsonSerialize(using = BigDecimalAsStringSerializer.class)
        @JsonDeserialize(using = BigDecimalAsStringSerializer.Deserializer.class)
        BigDecimal averageRating,
        long movieCount
) {
}
