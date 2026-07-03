package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.demo.imdb.repository.GenreMovieCount;

/** Everything the home page shows. */
public record HomeView(
        FeaturedMovie featured,
        List<Movie> recentlyViewed,
        List<Rating> topRated,
        List<GenreMovieCount> genreCounts,
        List<Watchlist> watchlistEntries
) {
}
