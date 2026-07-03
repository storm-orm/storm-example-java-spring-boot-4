package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Rating;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class RatingRepositoryTest {

    @Test
    void findTopRatedReturnsMoviesWithTheirFullGraphInOneQuery(ORMTemplate orm, SqlCapture capture) {
        RatingRepository ratingRepository = orm.repository(RatingRepository.class);
        List<Rating> topRated = capture.execute(() -> ratingRepository.findTopRated(1000, 3));
        TestSupport.printStatements(capture, "findTopRated");
        assertEquals(1, capture.count(Operation.SELECT));
        assertEquals(
                List.of("The Shawshank Redemption", "The Godfather", "Pulp Fiction"),
                topRated.stream().map(rating -> rating.movie().primaryTitle()).toList()
        );
    }

    @Test
    void findTopMoviesWithoutGenreSortsAllMoviesByRating(ORMTemplate orm, SqlCapture capture) {
        RatingRepository ratingRepository = orm.repository(RatingRepository.class);
        List<Rating> topMovies = capture.execute(() ->
                ratingRepository.findTopMovies(null, TopMoviesSort.RATING, 1000, 10));
        TestSupport.printStatements(capture, "findTopMovies-all");
        // No genre filter: the junction table must not be joined.
        assertTrue(!capture.statements().get(0).statement().contains("movie_genre"));
        assertEquals(5, topMovies.size());
        assertEquals("The Shawshank Redemption", topMovies.get(0).movie().primaryTitle());
    }

    @Test
    void findTopMoviesWithGenreJoinsTheJunctionTable(ORMTemplate orm, SqlCapture capture) {
        GenreRepository genreRepository = orm.repository(GenreRepository.class);
        RatingRepository ratingRepository = orm.repository(RatingRepository.class);
        Genre action = genreRepository.findByName("Action").orElseThrow();
        capture.clear();

        List<Rating> topActionMovies = capture.execute(() ->
                ratingRepository.findTopMovies(action, TopMoviesSort.RATING, 1000, 10));
        TestSupport.printStatements(capture, "findTopMovies-action");
        assertTrue(capture.statements().get(0).statement().contains("movie_genre"));
        assertEquals(
                List.of("The Matrix", "The Matrix Reloaded"),
                topActionMovies.stream().map(rating -> rating.movie().primaryTitle()).toList()
        );
    }

    @Test
    void findTopMoviesSortedByYearPutsNewestFirst(ORMTemplate orm, SqlCapture capture) {
        RatingRepository ratingRepository = orm.repository(RatingRepository.class);
        List<Rating> newestMovies = capture.execute(() ->
                ratingRepository.findTopMovies(null, TopMoviesSort.YEAR, 1000, 10));
        TestSupport.printStatements(capture, "findTopMovies-year");
        assertEquals("The Matrix Reloaded", newestMovies.get(0).movie().primaryTitle());
    }
}
