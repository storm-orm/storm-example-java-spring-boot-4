package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Rating;

/** Everything the top movies page shows. */
public record TopMoviesView(
        List<Genre> genres,
        Genre selectedGenre,
        List<Rating> entries
) {
}
