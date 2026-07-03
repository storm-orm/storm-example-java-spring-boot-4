package st.orm.demo.imdb.web;

import java.util.List;

public record SearchSuggestions(
        List<MovieSearchItem> movies,
        List<PersonSearchItem> persons
) {
}
