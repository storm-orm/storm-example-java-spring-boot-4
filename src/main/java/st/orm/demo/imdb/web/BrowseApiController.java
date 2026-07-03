package st.orm.demo.imdb.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import st.orm.Window;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.service.BrowseService;

@RestController
public class BrowseApiController {

    private final BrowseService browseService;

    public BrowseApiController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/api/browse/{genreName}")
    public SearchWindow<MovieSearchItem> browseMovies(@PathVariable String genreName,
                                                      @RequestParam(required = false) String cursor) {
        Window<MovieSummary> window = browseService.scrollGenre(genreName, cursor);
        if (window == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown genre: " + genreName);
        }
        return new SearchWindow<>(window.content().stream().map(MovieSearchItem::of).toList(), window.nextCursor());
    }
}
