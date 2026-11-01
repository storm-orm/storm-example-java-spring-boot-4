package st.orm.demo.imdb.service;

import st.orm.Window;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Person;

/** The first result windows of both search sections. */
public record SearchResults(
        Window<Movie> movieWindow,
        Window<Person> personWindow
) {
}
