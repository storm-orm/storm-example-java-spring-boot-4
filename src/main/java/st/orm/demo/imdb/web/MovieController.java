package st.orm.demo.imdb.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import st.orm.demo.imdb.service.MovieDetail;
import st.orm.demo.imdb.service.MovieService;

@Controller
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movie/{movieId}")
    public String movieDetail(@PathVariable String movieId, Model model) {
        MovieDetail detail = movieService.viewMovie(movieId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown movie: " + movieId);
        }
        model.addAttribute("detail", detail);
        return "movie";
    }
}
