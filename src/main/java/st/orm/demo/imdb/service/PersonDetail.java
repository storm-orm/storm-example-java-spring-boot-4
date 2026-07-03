package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.repository.FilmographyEntry;
import st.orm.demo.imdb.repository.PersonStatistics;

/** Everything the person detail page shows. */
public record PersonDetail(
        Person person,
        List<FilmographyEntry> filmography,
        PersonStatistics statistics
) {
}
