package st.orm.demo.imdb.repository;

import java.math.BigDecimal;
import st.orm.demo.imdb.model.Principal;

/**
 * Query result shape: one movie in a person's filmography — the credit
 * itself (which carries the movie and role) plus the movie's rating for
 * sorting and display. Not backed by a database table or view, so it is a
 * plain record — deliberately not a Data type.
 */
public record FilmographyEntry(
        Principal principal,
        BigDecimal averageRating
) {
}
