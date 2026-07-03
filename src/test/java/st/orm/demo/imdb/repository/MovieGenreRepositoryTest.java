package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieGenreRepositoryTest {

    @Test
    void findGenresReturnsTheGenresOfOneMovie(ORMTemplate orm, SqlCapture capture) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        MovieGenreRepository movieGenreRepository = orm.repository(MovieGenreRepository.class);
        Movie matrix = movieRepository.getById("tt0133093");
        capture.clear();
        List<Genre> genres = capture.execute(() -> movieGenreRepository.findGenres(matrix));
        TestSupport.printStatements(capture, "findGenres");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(List.of("Action", "Sci-Fi"), genres.stream().map(Genre::name).toList());
    }

    @Test
    void findGenreRatingStatisticsFiltersGroupsWithHaving(ORMTemplate orm, SqlCapture capture) {
        MovieGenreRepository movieGenreRepository = orm.repository(MovieGenreRepository.class);
        List<GenreRatingStatistics> statistics = capture.execute(() ->
                movieGenreRepository.findGenreRatingStatistics(3, 10));
        TestSupport.printStatements(capture, "findGenreRatingStatistics");
        assertTrue(capture.statements().get(0).statement().contains("HAVING"));
        // Only Drama has 3+ rated movies; average of 9.3, 8.9, 9.2 is ~9.13.
        GenreRatingStatistics drama = statistics.get(0);
        assertEquals(1, statistics.size());
        assertEquals("Drama", drama.genre().name());
        assertEquals(3L, drama.movieCount());
        assertTrue(drama.averageRating().compareTo(new BigDecimal("9.0")) > 0
                && drama.averageRating().compareTo(new BigDecimal("9.2")) < 0);
    }
}
