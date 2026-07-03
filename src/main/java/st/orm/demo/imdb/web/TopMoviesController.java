package st.orm.demo.imdb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import st.orm.demo.imdb.repository.TopMoviesSort;
import st.orm.demo.imdb.service.TopMoviesService;
import st.orm.demo.imdb.service.TopMoviesView;

@Controller
public class TopMoviesController {

    private final TopMoviesService topMoviesService;

    public TopMoviesController(TopMoviesService topMoviesService) {
        this.topMoviesService = topMoviesService;
    }

    @GetMapping("/top")
    public String topMovies(@RequestParam(required = false) String genre,
                            @RequestParam(defaultValue = "rating") String sort,
                            Model model) {
        TopMoviesSort sortBy = "year".equals(sort) ? TopMoviesSort.YEAR : TopMoviesSort.RATING;
        TopMoviesView view = topMoviesService.findTopMovies(genre, sortBy);
        model.addAttribute("genres", view.genres());
        model.addAttribute("selectedGenre", view.selectedGenre());
        model.addAttribute("sort", sortBy == TopMoviesSort.YEAR ? "year" : "rating");
        model.addAttribute("entries", view.entries());
        return "top";
    }
}
