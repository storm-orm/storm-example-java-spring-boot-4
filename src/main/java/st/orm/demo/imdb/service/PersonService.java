package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import st.orm.Ref;
import st.orm.demo.imdb.model.Movie;
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
    public PersonDetail findPersonDetail(Ref<Person> personRef) {
        return transaction(READ_ONLY, tx -> {
            Person person = personRepository.findByRef(personRef).orElse(null);
            if (person == null) {
                return null;
            }
            // A person can hold multiple credits in one movie; show each movie once.
            // The query returns every credit: one row per movie would have to pick
            // one of its credits, which no grouping can, and the rows are few.
            Set<Ref<Movie>> seenMovies = new LinkedHashSet<>();
            List<FilmographyEntry> filmography = principalRepository.findFilmography(person).stream()
                    .filter(entry -> seenMovies.add(Ref.of(entry.movie())))
                    .toList();
            return new PersonDetail(
                    person,
                    filmography,
                    principalRepository.findStatistics(person)
            );
            });
    }
}
