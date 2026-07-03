package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Rating;

/** The featured hero section: the most recently viewed movie with context. */
public record FeaturedMovie(
        Movie movie,
        Rating rating,
        List<Genre> genres
) {
}
