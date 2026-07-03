package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;

import java.util.List;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Genre_;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieGenrePk;
import st.orm.demo.imdb.model.MovieGenre_;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Rating_;
import st.orm.repository.EntityRepository;

public interface MovieGenreRepository extends EntityRepository<MovieGenre, MovieGenrePk> {

    /** The genres of a single movie, for the movie detail page. */
    default List<Genre> findGenres(Movie movie) {
        return select(Genre.class)
                .where(MovieGenre_.movie, movie)
                .orderByAny(Genre_.name)
                .getResultList();
    }

    /**
     * Top genres by average rating for the statistics page. Demonstrates
     * GROUP BY + HAVING: genres qualify only with enough rated movies.
     * The aggregate expressions live in templates; everything else is code.
     */
    default List<GenreRatingStatistics> findGenreRatingStatistics(int minimumMovieCount, int limit) {
        return select(GenreRatingStatistics.class, RAW."\{Genre.class}, AVG(\{Rating_.averageRating}), COUNT(*)")
                .innerJoin(Rating.class).on(Movie.class)
                .groupByAny(Genre_.id, Genre_.name)
                .having(RAW."COUNT(*) >= \{minimumMovieCount}")
                .orderByDescending(RAW."AVG(\{Rating_.averageRating})")
                .limit(limit)
                .getResultList();
    }
}
