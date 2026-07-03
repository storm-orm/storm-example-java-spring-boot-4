package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Principal;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class PrincipalRepositoryTest {

    @Test
    void findCastReturnsTheFullGraphInBillingOrderWithOneQuery(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        PrincipalRepository principalRepository = orm.repository(PrincipalRepository.class);
        Movie matrix = movieRepository.getById("tt0133093");
        capture.clear();
        List<Principal> cast = capture.execute(() -> principalRepository.findCast(matrix));
        TestSupport.printStatements(capture, "findCast");
        // One query loads the credits including their movie and person graph.
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(List.of("Keanu Reeves", "Laurence Fishburne"),
                cast.stream().map(principal -> principal.person().primaryName()).toList());
        assertEquals(List.of("Neo", "Morpheus"), cast.stream().map(Principal::characters).toList());
        assertEquals("The Matrix", cast.get(0).movie().primaryTitle());
    }

    @Test
    void findFilmographySortsByRatingDescending(ORMTemplate orm, SqlCapture capture) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        PrincipalRepository principalRepository = orm.repository(PrincipalRepository.class);
        Person keanuReeves = personRepository.getById("nm0000206");
        capture.clear();
        List<FilmographyEntry> filmography = capture.execute(() -> principalRepository.findFilmography(keanuReeves));
        TestSupport.printStatements(capture, "findFilmography");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(
                List.of(
                        Map.entry("The Matrix", new BigDecimal("8.7")),
                        Map.entry("The Matrix Reloaded", new BigDecimal("7.2"))
                ),
                filmography.stream()
                        .map(entry -> Map.entry(entry.principal().movie().primaryTitle(), entry.averageRating()))
                        .toList()
        );
    }

    @Test
    void findStatisticsAggregatesMovieCountAndAverageRating(ORMTemplate orm, SqlCapture capture) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        PrincipalRepository principalRepository = orm.repository(PrincipalRepository.class);
        Person keanuReeves = personRepository.getById("nm0000206");
        capture.clear();
        PersonStatistics statistics = capture.execute(() -> principalRepository.findStatistics(keanuReeves));
        TestSupport.printStatements(capture, "findStatistics");
        assertEquals(2L, statistics.movieCount());
        // Average of 8.7 and 7.2 is 7.95.
        assertEquals(0, new BigDecimal("7.95").compareTo(statistics.averageRating()));
    }

    @Test
    void findMoviesSharingCastRanksBySharedCastMembers(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        PrincipalRepository principalRepository = orm.repository(PrincipalRepository.class);
        Movie matrix = movieRepository.getById("tt0133093");
        List<Person> castMembers = personRepository.findAllById(List.of("nm0000206", "nm0000401"));
        capture.clear();
        List<RelatedMovie> relatedMovies = capture.execute(() ->
                principalRepository.findMoviesSharingCast(castMembers, matrix, 6));
        TestSupport.printStatements(capture, "findMoviesSharingCast");
        assertTrue(capture.statements().get(0).statement().contains("GROUP BY"));
        // The Matrix Reloaded shares both cast members with The Matrix.
        assertEquals(1, relatedMovies.size());
        RelatedMovie related = relatedMovies.get(0);
        assertEquals("The Matrix Reloaded", related.movie().primaryTitle());
        assertEquals(2L, related.sharedCastCount());
    }

    @Test
    void findMostProlificActorsCountsCreditsPerPerson(ORMTemplate orm, SqlCapture capture) {
        PrincipalRepository principalRepository = orm.repository(PrincipalRepository.class);
        List<ProlificActor> prolificActors = capture.execute(() -> principalRepository.findMostProlificActors(10));
        TestSupport.printStatements(capture, "findMostProlificActors");
        // Keanu Reeves and Laurence Fishburne appear in two movies each.
        assertEquals(
                Set.of("Keanu Reeves", "Laurence Fishburne"),
                prolificActors.stream().limit(2).map(actor -> actor.person().primaryName()).collect(java.util.stream.Collectors.toSet())
        );
        assertTrue(prolificActors.stream().limit(2).allMatch(actor -> actor.movieCount() == 2L));
    }
}
