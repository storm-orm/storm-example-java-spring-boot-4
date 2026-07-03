package st.orm.demo.imdb.repository;

import static st.orm.Operator.GREATER_THAN_OR_EQUAL;
import static st.orm.Operator.IS_NOT_NULL;

import java.util.List;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieGenre_;
import st.orm.demo.imdb.model.Movie_;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Rating_;
import st.orm.repository.EntityRepository;
import st.orm.template.QueryBuilder;

public interface RatingRepository extends EntityRepository<Rating, Movie> {

    /**
     * The highest rated movies. The vote floor keeps obscure titles with a
     * handful of enthusiastic voters out of the list. Each rating carries
     * its full movie via the entity graph — one query, no N+1.
     */
    default List<Rating> findTopRated(int minimumVoteCount, int limit) {
        return select()
                .where(Rating_.voteCount, GREATER_THAN_OR_EQUAL, minimumVoteCount)
                .orderByDescending(Rating_.averageRating)
                .limit(limit)
                .getResultList();
    }

    /**
     * The top movies page: optionally filtered by genre, sorted by rating
     * or release year. Demonstrates query composition — the genre join and
     * the sort order are decided by plain Java conditionals as the query
     * builder is assembled.
     */
    default List<Rating> findTopMovies(Genre genre, TopMoviesSort sortBy, int minimumVoteCount, int limit) {
        QueryBuilder<Rating, Rating, Movie> query = select()
                .where(Rating_.voteCount, GREATER_THAN_OR_EQUAL, minimumVoteCount);
        if (genre != null) {
            query = query.innerJoin(MovieGenre.class).on(Movie.class)
                    .whereAny(predicate -> predicate.whereAny(MovieGenre_.genre, genre));
        }
        query = switch (sortBy) {
            case RATING -> query.orderByDescending(Rating_.averageRating);
            case YEAR -> query
                    .whereAny(predicate -> predicate.whereAny(Movie_.startYear, IS_NOT_NULL))
                    .orderByDescendingAny(Movie_.startYear, Rating_.averageRating);
        };
        return query.limit(limit).getResultList();
    }
}
