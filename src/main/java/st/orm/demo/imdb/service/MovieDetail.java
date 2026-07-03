package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.repository.RelatedMovie;

/** Everything the movie detail page shows. */
public record MovieDetail(
        Movie movie,
        Rating rating,
        List<Genre> genres,
        List<Principal> cast,
        List<RelatedMovie> relatedMovies,
        boolean onWatchlist
) {
}
