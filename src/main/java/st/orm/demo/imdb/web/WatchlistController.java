package st.orm.demo.imdb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import st.orm.Page;
import st.orm.demo.imdb.model.Watchlist;
import st.orm.demo.imdb.service.WatchlistService;

@Controller
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/watchlist")
    public String watchlist(@RequestParam(defaultValue = "1") int page, Model model) {
        // Page numbers in the URL are 1-based; Storm pages are 0-based.
        Page<Watchlist> watchlistPage = watchlistService.findPage(Math.max(page - 1, 0));
        model.addAttribute("watchlistPage", watchlistPage);
        model.addAttribute("currentPage", page);
        return "watchlist";
    }
}
