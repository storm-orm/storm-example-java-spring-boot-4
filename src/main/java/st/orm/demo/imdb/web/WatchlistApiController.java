package st.orm.demo.imdb.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import st.orm.demo.imdb.service.WatchlistService;

@RestController
public class WatchlistApiController {

    private final WatchlistService watchlistService;

    public WatchlistApiController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/api/watchlist/{movieId}")
    public WatchlistState toggle(@PathVariable String movieId) {
        return new WatchlistState(watchlistService.toggle(movieId));
    }
}
