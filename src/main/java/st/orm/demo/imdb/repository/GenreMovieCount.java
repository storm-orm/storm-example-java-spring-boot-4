package st.orm.demo.imdb.repository;

import st.orm.demo.imdb.model.Genre;

/**
 * Query result shape: a genre together with the number of movies in it,
 * for the genre navigation bar on the home page. Not backed by a database
 * table or view, so it is a plain record — deliberately not a Data type.
 */
public record GenreMovieCount(
        Genre genre,
        long movieCount
) {
}
