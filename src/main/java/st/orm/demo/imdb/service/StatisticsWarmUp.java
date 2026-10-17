package st.orm.demo.imdb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The statistics page aggregates every movie, rating and credit, which takes a
 * moment, and its cache lives in memory, so it starts empty after each restart.
 * Filling it in the background once the application is ready, after the import
 * has put the data in place, spares the first visitor that wait. A failure is
 * logged, and the page computes the view itself.
 */
@Component
public class StatisticsWarmUp {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsWarmUp.class);

    private final StatisticsService statisticsService;

    public StatisticsWarmUp(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        Thread.ofVirtual().name("statistics-warm-up").start(() -> {
            try {
                statisticsService.buildStatisticsView();
            } catch (RuntimeException e) {
                logger.warn("Could not prepare the statistics in advance.", e);
            }
        });
    }
}
