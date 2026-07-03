package st.orm.demo.imdb.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import st.orm.demo.imdb.service.BrowseService;
import st.orm.demo.imdb.service.BrowseView;

@Controller
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/browse/{genreName}")
    public String browse(@PathVariable String genreName, Model model) {
        BrowseView view = browseService.browseGenre(genreName);
        if (view == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown genre: " + genreName);
        }
        model.addAttribute("genre", view.genre());
        model.addAttribute("movieCount", view.movieCount());
        model.addAttribute("movieWindow", view.movieWindow());
        return "browse";
    }
}
