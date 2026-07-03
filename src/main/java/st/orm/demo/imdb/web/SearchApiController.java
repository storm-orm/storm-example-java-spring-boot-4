package st.orm.demo.imdb.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import st.orm.Window;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.PersonSummary;
import st.orm.demo.imdb.service.SearchService;
import st.orm.demo.imdb.service.Suggestions;

@RestController
public class SearchApiController {

    private final SearchService searchService;

    public SearchApiController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search/suggestions")
    public SearchSuggestions suggestions(@RequestParam String query) {
        Suggestions suggestions = searchService.findSuggestions(query);
        return new SearchSuggestions(
                suggestions.movies().stream().map(MovieSearchItem::of).toList(),
                suggestions.persons().stream().map(PersonSearchItem::of).toList()
        );
    }

    @GetMapping("/api/search/movies")
    public SearchWindow<MovieSearchItem> movies(@RequestParam String query,
                                                @RequestParam(required = false) String cursor) {
        Window<MovieSummary> window = searchService.scrollMovies(query, cursor);
        return new SearchWindow<>(window.content().stream().map(MovieSearchItem::of).toList(), window.nextCursor());
    }

    @GetMapping("/api/search/persons")
    public SearchWindow<PersonSearchItem> persons(@RequestParam String query,
                                                  @RequestParam(required = false) String cursor) {
        Window<PersonSummary> window = searchService.scrollPersons(query, cursor);
        return new SearchWindow<>(window.content().stream().map(PersonSearchItem::of).toList(), window.nextCursor());
    }
}
