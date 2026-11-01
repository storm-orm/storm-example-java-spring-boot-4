package st.orm.demo.imdb.repository;

import static st.orm.Operator.EQUALS_IGNORE_CASE;
import static st.orm.Operator.STARTS_WITH_IGNORE_CASE;
import static st.orm.demo.imdb.repository.SearchText.folded;
import static st.orm.demo.imdb.repository.SearchText.searchText;

import java.util.List;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.PersonSummary_;
import st.orm.repository.ProjectionRepository;

public interface PersonSummaryRepository extends ProjectionRepository<PersonSummary, String> {

    /**
     * Name search with keyset scrolling. Every word of the query must occur
     * in the name, ignoring case and accents, so "reeves keanu" finds Keanu
     * Reeves and "penelope cruz" finds Penélope Cruz. The database removes the
     * accents from the names and the query alike ({@link SearchText#folded}),
     * through the expression its trigram index is built on. A name equal to
     * the query comes first and a name starting with it next; scroll follows
     * the ranks.
     */
    default Window<PersonSummary> searchByName(String query, Scrollable<PersonSummary> scrollable) {
        String name = searchText(query);
        return select()
                .where(it -> it.search(name, folded(PersonSummary_.primaryName)))
                .orderFirst(it -> it.where(folded(PersonSummary_.primaryName), EQUALS_IGNORE_CASE, name))
                .orderFirst(it -> it.where(folded(PersonSummary_.primaryName), STARTS_WITH_IGNORE_CASE, name))
                .scroll(scrollable);
    }

    /**
     * Name suggestions for the search auto-complete. Like the movie
     * suggestions it uses suggest, so every keystroke reads the trigram
     * index of V1: a word of three letters or more matched anywhere in a
     * name and a word of two at the start of one, so in "de niro" "niro"
     * matches anywhere and "de" at the start of a word. A word of one letter
     * takes no part, so an input of such words alone suggests nothing. No
     * words are ignored: the particles of names, such as "de" and "van",
     * are what the user types to find De Niro or Van Damme. They are ranked
     * as {@link #searchByName} ranks them, alphabetically within each rank.
     */
    default List<PersonSummary> findNameSuggestions(String query, int limit) {
        String name = searchText(query);
        return select()
                .where(it -> it.suggest(name, folded(PersonSummary_.primaryName)))
                .orderFirst(it -> it.where(folded(PersonSummary_.primaryName), EQUALS_IGNORE_CASE, name))
                .orderFirst(it -> it.where(folded(PersonSummary_.primaryName), STARTS_WITH_IGNORE_CASE, name))
                .orderBy(PersonSummary_.primaryName)
                .limit(limit)
                .getResultList();
    }
}
