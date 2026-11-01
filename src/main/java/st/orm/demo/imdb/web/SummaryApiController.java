package st.orm.demo.imdb.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import st.orm.Ref;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.service.ExternalSummary;
import st.orm.demo.imdb.service.WikipediaSummaryService;

/**
 * Wikipedia enrichment for the detail pages. The pages fetch these
 * summaries asynchronously and render fine without them.
 */
@RestController
public class SummaryApiController {

    private final WikipediaSummaryService wikipediaSummaryService;

    public SummaryApiController(WikipediaSummaryService wikipediaSummaryService) {
        this.wikipediaSummaryService = wikipediaSummaryService;
    }

    @GetMapping("/api/summary/movie/{movieId}")
    public ResponseEntity<ExternalSummary> movieSummary(@PathVariable String movieId) {
        ExternalSummary summary = wikipediaSummaryService.findMovieSummary(Ref.of(Movie.class, movieId));
        return summary != null ? ResponseEntity.ok(summary) : ResponseEntity.notFound().build();
    }

    @GetMapping("/api/summary/person/{personId}")
    public ResponseEntity<ExternalSummary> personSummary(@PathVariable String personId) {
        ExternalSummary summary = wikipediaSummaryService.findPersonSummary(Ref.of(Person.class, personId));
        return summary != null ? ResponseEntity.ok(summary) : ResponseEntity.notFound().build();
    }
}
