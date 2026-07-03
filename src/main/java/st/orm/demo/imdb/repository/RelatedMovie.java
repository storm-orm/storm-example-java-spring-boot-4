package st.orm.demo.imdb.repository;

import st.orm.demo.imdb.model.Movie;

/**
 * Query result shape: a movie related to another movie through shared cast
 * members, ranked by how many cast members the two movies have in common.
 * Not backed by a database table or view, so it is a plain record —
 * deliberately not a Data type.
 */
public record RelatedMovie(
        Movie movie,
        long sharedCastCount
) {
}
