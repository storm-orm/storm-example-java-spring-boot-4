package st.orm.demo.imdb.repository;

import java.util.List;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.PersonSummary_;
import st.orm.repository.ProjectionRepository;

public interface PersonSummaryRepository extends ProjectionRepository<PersonSummary, String> {

    /**
     * Name search with keyset scrolling. Every word of the query must occur
     * in the name, ignoring case, so "reeves keanu" finds Keanu Reeves.
     */
    default Window<PersonSummary> searchByName(String query, Scrollable<PersonSummary> scrollable) {
        return select()
                .where(it -> it.search(query, PersonSummary_.primaryName))
                .scroll(scrollable);
    }

    /** Name suggestions for the search auto-complete. */
    default List<PersonSummary> findNameSuggestions(String query, int limit) {
        return select()
                .where(it -> it.search(query, PersonSummary_.primaryName))
                .orderBy(PersonSummary_.primaryName)
                .limit(limit)
                .getResultList();
    }
}
