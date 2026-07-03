package st.orm.demo.imdb.repository;

import java.util.List;
import st.orm.Page;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.demo.imdb.model.Watchlist_;
import st.orm.repository.EntityRepository;

public interface WatchlistRepository extends EntityRepository<Watchlist, Movie> {

    /**
     * One page of the watchlist, newest first, with offset-based pagination.
     * Page numbers are 0-based.
     */
    default Page<Watchlist> findPage(int pageNumber, int pageSize) {
        return select()
                .orderByDescending(Watchlist_.addedAt)
                .page(pageNumber, pageSize);
    }

    /** The most recently added watchlist entries for the home page. */
    default List<Watchlist> findMostRecent(int limit) {
        return select()
                .orderByDescending(Watchlist_.addedAt)
                .limit(limit)
                .getResultList();
    }
}
