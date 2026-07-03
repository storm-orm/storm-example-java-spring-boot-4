package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.PersonSummary;

/** Auto-complete suggestions for both types. */
public record Suggestions(
        List<MovieSummary> movies,
        List<PersonSummary> persons
) {
}
