package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Genre;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class GenreRepositoryTest {

    @Test
    void findByNameLooksUpByTheUniqueKey(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        Optional<Genre> sciFi = capture.execute(() -> genreRepository.findByName("Sci-Fi"));
        TestSupport.printStatements(capture, "findByName");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals("Sci-Fi", sciFi.map(Genre::name).orElse(null));
        assertTrue(genreRepository.findByName("Musical").isEmpty());
    }

    @Test
    void findGenresWithMovieCountsAggregatesOverTheJunctionTable(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        List<GenreMovieCount> genreCounts = capture.execute(genreRepository::findGenresWithMovieCounts);
        TestSupport.printStatements(capture, "findGenresWithMovieCounts");
        assertEquals(1, capture.count(Operation.SELECT));
        assertTrue(capture.statements().get(0).statement().contains("GROUP BY"));
        assertEquals(
                List.of(Map.entry("Action", 2L), Map.entry("Drama", 3L), Map.entry("Sci-Fi", 2L)),
                genreCounts.stream().map(count -> Map.entry(count.genre().name(), count.movieCount())).toList()
        );
    }
}
