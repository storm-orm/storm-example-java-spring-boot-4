package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;
import static st.orm.Operator.IS_NOT_NULL;

import java.util.List;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Movie_;
import st.orm.repository.EntityRepository;

public interface MovieRepository extends EntityRepository<Movie, String> {

    /**
     * Movies grouped per decade for the statistics page. The decade bucket
     * is a computed expression, so the SELECT and GROUP BY use a template —
     * still with metamodel column references.
     */
    default List<DecadeMovieCount> countMoviesPerDecade() {
        return select(DecadeMovieCount.class, RAW."(\{Movie_.startYear} / 10) * 10, COUNT(*)")
                .where(Movie_.startYear, IS_NOT_NULL)
                .groupBy(RAW."(\{Movie_.startYear} / 10) * 10")
                .orderBy(RAW."(\{Movie_.startYear} / 10) * 10")
                .getResultList();
    }
}
