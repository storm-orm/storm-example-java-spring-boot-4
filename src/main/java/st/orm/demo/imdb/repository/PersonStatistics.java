package st.orm.demo.imdb.repository;

import java.math.BigDecimal;

/**
 * Query result shape: aggregate statistics for a person's filmography,
 * shown on the person detail page. The average is null for persons without
 * rated movies. Not backed by a database table or view, so it is a plain
 * record — deliberately not a Data type.
 */
public record PersonStatistics(
        long movieCount,
        BigDecimal averageRating
) {
}
