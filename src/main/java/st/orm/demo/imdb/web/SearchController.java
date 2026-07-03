package st.orm.demo.imdb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import st.orm.demo.imdb.service.SearchResults;
import st.orm.demo.imdb.service.SearchService;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Model model) {
        String searchQuery = query != null ? query.trim() : "";
        model.addAttribute("query", searchQuery);
        if (!searchQuery.isEmpty()) {
            SearchResults results = searchService.search(searchQuery);
            model.addAttribute("movieWindow", results.movieWindow());
            model.addAttribute("personWindow", results.personWindow());
        }
        return "search";
    }
}
