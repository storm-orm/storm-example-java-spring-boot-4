package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.repository.FilmographyEntry;
import st.orm.demo.imdb.repository.PersonRepository;
import st.orm.demo.imdb.repository.PrincipalRepository;

@Service
public class PersonService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    private final PersonRepository personRepository;
    private final PrincipalRepository principalRepository;

    public PersonService(PersonRepository personRepository, PrincipalRepository principalRepository) {
        this.personRepository = personRepository;
        this.principalRepository = principalRepository;
    }

    /** The person page: filmography and statistics in one read-only transaction. */
    public PersonDetail findPersonDetail(String personId) {
        return transaction(READ_ONLY, tx -> {
            Person person = personRepository.findById(personId).orElse(null);
            if (person == null) {
                return null;
            }
            // A person can hold multiple credits in one movie; show each movie once.
            Set<String> seenMovieIds = new LinkedHashSet<>();
            List<FilmographyEntry> filmography = principalRepository.findFilmography(person).stream()
                    .filter(entry -> seenMovieIds.add(entry.principal().movie().id()))
                    .toList();
            return new PersonDetail(
                    person,
                    filmography,
                    principalRepository.findStatistics(person)
            );
            });
    }
}
