package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.model.PersonSummary_;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class PersonSummaryRepositoryTest {

    @Test
    void searchByNameSelectsOnlyTheProjectedColumns(ORMTemplate orm, SqlCapture capture) {
        PersonSummaryRepository personSummaryRepository = orm.repository(PersonSummaryRepository.class);
        Window<PersonSummary> window = capture.execute(() ->
                personSummaryRepository.searchByName("FREEMAN", Scrollable.of(PersonSummary_.id, 10)));
        TestSupport.printStatements(capture, "searchByName");
        assertEquals(1, capture.count(Operation.SELECT));
        String statement = capture.statements().get(0).statement();
        // The projection narrows the SELECT: no birth/death year columns.
        assertFalse(statement.contains("birth_year"));
        assertFalse(statement.contains("death_year"));
        assertEquals(List.of("Morgan Freeman"), window.content().stream().map(PersonSummary::primaryName).toList());
    }

    @Test
    void findNameSuggestionsOrdersByName(ORMTemplate orm, SqlCapture capture) {
        PersonSummaryRepository personSummaryRepository = orm.repository(PersonSummaryRepository.class);
        List<PersonSummary> suggestions = capture.execute(() ->
                personSummaryRepository.findNameSuggestions("an", 10));
        TestSupport.printStatements(capture, "findNameSuggestions");
        assertEquals(List.of("Keanu Reeves", "Morgan Freeman"),
                suggestions.stream().map(PersonSummary::primaryName).toList());
    }
}
