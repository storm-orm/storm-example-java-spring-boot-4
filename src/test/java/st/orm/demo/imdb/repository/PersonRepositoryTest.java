package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Person_;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class PersonRepositoryTest {

    @Test
    void searchByNameReadsTheMatchingPeopleInOneStatement(ORMTemplate orm, SqlCapture capture) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        Window<Person> window = capture.execute(() ->
                personRepository.searchByName("FREEMAN", Scrollable.of(Person_.id, 10)));
        TestSupport.printStatements(capture, "searchByName");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(List.of("Morgan Freeman"), window.content().stream().map(Person::primaryName).toList());
    }

    @Test
    void findNameSuggestionsOrdersByName(ORMTemplate orm, SqlCapture capture) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        List<Person> suggestions = capture.execute(() ->
                personRepository.findNameSuggestions("ree", 10));
        TestSupport.printStatements(capture, "findNameSuggestions");
        assertEquals(List.of("Keanu Reeves", "Morgan Freeman"),
                suggestions.stream().map(Person::primaryName).toList());
    }

    @Test
    void findNameSuggestionsMatchesATwoLetterWordAtTheStartOfAWord(ORMTemplate orm) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        // "fr" starts the second word of Morgan Freeman's name.
        assertEquals(List.of("Morgan Freeman"), suggestedNames(personRepository, "fr"));
        // "an" is in Keanu and Morgan, but starts no word.
        assertEquals(List.of(), suggestedNames(personRepository, "an"));
        // A word of one letter takes no part, so it suggests nothing.
        assertEquals(List.of(), suggestedNames(personRepository, "k"));
    }

    private static List<String> suggestedNames(PersonRepository personRepository, String query) {
        return personRepository.findNameSuggestions(query, 10)
                .stream().map(Person::primaryName).toList();
    }

    @Test
    void searchByNameMatchesEveryWordOfTheQueryInAnyOrder(ORMTemplate orm) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        assertEquals(List.of("Keanu Reeves"), names(personRepository, "reeves keanu"));
        assertEquals(List.of(), names(personRepository, "keanu freeman"));
    }

    private static List<String> names(PersonRepository personRepository, String query) {
        return personRepository.searchByName(query, Scrollable.of(Person_.id, 10))
                .content().stream().map(Person::primaryName).toList();
    }
}
