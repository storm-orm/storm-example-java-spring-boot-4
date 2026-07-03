package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;

import java.util.List;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.PersonSummary_;
import st.orm.repository.ProjectionRepository;

public interface PersonSummaryRepository extends ProjectionRepository<PersonSummary, String> {

    /** Case-insensitive name search with keyset scrolling. */
    default Window<PersonSummary> searchByName(String query, Scrollable<PersonSummary> scrollable) {
        String pattern = "%" + query + "%";
        return select()
                .where(RAW."LOWER(\{PersonSummary_.primaryName}) LIKE LOWER(\{pattern})")
                .scroll(scrollable);
    }

    /** Name suggestions for the search auto-complete. */
    default List<PersonSummary> findNameSuggestions(String query, int limit) {
        String pattern = "%" + query + "%";
        return select()
                .where(RAW."LOWER(\{PersonSummary_.primaryName}) LIKE LOWER(\{pattern})")
                .orderBy(PersonSummary_.primaryName)
                .limit(limit)
                .getResultList();
    }
}
