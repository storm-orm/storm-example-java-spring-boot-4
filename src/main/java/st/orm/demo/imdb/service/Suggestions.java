package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Person;

/** Auto-complete suggestions for both types. */
public record Suggestions(
        List<Movie> movies,
        List<Person> persons
) {
}
