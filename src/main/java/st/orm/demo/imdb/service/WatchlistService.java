package st.orm.demo.imdb.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.orm.Page;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.WatchlistRepository;

@Service
public class WatchlistService {

    private static final int WATCHLIST_PAGE_SIZE = 12;

    private final MovieRepository movieRepository;
    private final WatchlistRepository watchlistRepository;

    public WatchlistService(MovieRepository movieRepository, WatchlistRepository watchlistRepository) {
        this.movieRepository = movieRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /**
     * Adds the movie to the watchlist, or removes it when already present.
     * Returns whether the movie is on the watchlist afterwards.
     *
     * <p>The exists/insert/remove cycle runs in one transaction; in Spring Boot
     * the transaction delegates to Spring's transaction manager.
     */
    @Transactional
    public boolean toggle(String movieId) {
        Movie movie = movieRepository.getById(movieId);
        if (watchlistRepository.existsById(movie)) {
            watchlistRepository.removeById(movie);
            return false;
        }
        watchlistRepository.insert(new Watchlist(movie, Instant.now()));
        return true;
    }

    /** One page of the watchlist for the watchlist page (0-based). */
    public Page<Watchlist> findPage(int pageNumber) {
        return watchlistRepository.findPage(pageNumber, WATCHLIST_PAGE_SIZE);
    }
}
