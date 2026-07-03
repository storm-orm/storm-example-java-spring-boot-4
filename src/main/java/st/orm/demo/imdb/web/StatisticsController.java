package st.orm.demo.imdb.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import st.orm.demo.imdb.service.StatisticsService;
import st.orm.demo.imdb.service.StatisticsView;

@Controller
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        StatisticsView view = statisticsService.buildStatisticsView();
        model.addAttribute("decades", view.decades());
        model.addAttribute("maxDecadeCount", view.maxDecadeCount());
        model.addAttribute("genreStatistics", view.genreStatistics());
        model.addAttribute("prolificActors", view.prolificActors());
        return "statistics";
    }
}
