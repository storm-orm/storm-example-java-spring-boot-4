package st.orm.demo.imdb.repository;

import java.util.List;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.model.MovieView_;
import st.orm.repository.EntityRepository;

public interface MovieViewRepository extends EntityRepository<MovieView, Long> {

    /**
     * The most recent movie views, newest first. Views hold a Ref to their
     * movie, so this query stays on the view table alone — callers dedupe
     * the refs and fetch the movies they actually need.
     */
    default List<MovieView> findRecentViews(int limit) {
        return select()
                .orderByDescending(MovieView_.viewedAt, MovieView_.id)
                .limit(limit)
                .getResultList();
    }
}
