package st.orm.demo.imdb.service;

import st.orm.Window;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.PersonSummary;

/** The first result windows of both search sections. */
public record SearchResults(
        Window<MovieSummary> movieWindow,
        Window<PersonSummary> personWindow
) {
}
