package st.orm.demo.imdb.repository;

/**
 * Query result shape: the number of movies released in one decade,
 * e.g. 1990 -> 2412. Not backed by a database table or view, so it is a
 * plain record — deliberately not a Data type.
 */
public record DecadeMovieCount(
        int decade,
        long movieCount
) {
}
