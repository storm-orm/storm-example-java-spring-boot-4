package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.template.ORMTemplate;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieRepositoryTest {

    @Test
    void countMoviesPerDecadeGroupsByTheComputedDecade(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        List<DecadeMovieCount> decades = capture.execute(movieRepository::countMoviesPerDecade);
        TestSupport.printStatements(capture, "countMoviesPerDecade");
        assertTrue(capture.statements().get(0).statement().contains("GROUP BY"));
        assertEquals(
                List.of(Map.entry(1970, 1L), Map.entry(1990, 3L), Map.entry(2000, 1L)),
                decades.stream().map(decade -> Map.entry(decade.decade(), decade.movieCount())).toList()
        );
    }
}
