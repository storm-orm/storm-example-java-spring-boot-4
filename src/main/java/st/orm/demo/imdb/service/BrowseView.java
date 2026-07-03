package st.orm.demo.imdb.service;

import st.orm.Window;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.MovieSummary;

/** Everything the browse-by-genre page shows. */
public record BrowseView(
        Genre genre,
        long movieCount,
        Window<MovieSummary> movieWindow
) {
}
