package st.orm.demo.imdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.PrincipalRepository;
import st.orm.demo.imdb.serialization.JacksonSerializedCache;
import st.orm.demo.imdb.service.StatisticsService;
import st.orm.demo.imdb.service.StatisticsView;
import st.orm.jackson.StormModule;
import st.orm.template.ORMTemplate;
import st.orm.test.StormTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves that Storm entities cache safely through full serialization: the
 * statistics view — aggregation results carrying Genre and Person entities —
 * round-trips through JSON without loss, and the Spring cache stores JSON
 * strings rather than object references.
 */
@StormTest(scripts = {"/schema.sql", "/data.sql"})
class StatisticsCachingTest {

    @Test
    void theStatisticsViewRoundTripsThroughJsonWithoutLoss(ORMTemplate orm) {
        StatisticsView original = statisticsService(orm).buildStatisticsView();

        ObjectMapper objectMapper = JsonMapper.builder().addModule(new StormModule()).build();
        String payload = objectMapper.writeValueAsString(original);
        StatisticsView decoded = objectMapper.readValue(payload, StatisticsView.class);

        // Immutable records compare by value: the decoded copy is
        // indistinguishable from the queried original — entities included.
        assertEquals(original, decoded);
    }

    @Test
    void theSpringCacheStoresTheViewAsSerializedJson(ORMTemplate orm) {
        StatisticsView view = statisticsService(orm).buildStatisticsView();
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new StormModule()).build();
        JacksonSerializedCache cache = new JacksonSerializedCache(
                StatisticsService.STATISTICS_CACHE, StatisticsView.class, objectMapper);

        cache.put("statistics", view);

        // The native store holds a JSON string — like Redis would — not an
        // object reference; reading decodes it back into an equal value.
        assertInstanceOf(String.class, cache.getNativeCache().get("statistics"));
        assertEquals(view, cache.get("statistics", StatisticsView.class));
    }

    private StatisticsService statisticsService(ORMTemplate orm) {
        return new StatisticsService(
                orm.repository(MovieRepository.class),
                orm.repository(MovieGenreRepository.class),
                orm.repository(PrincipalRepository.class)
        );
    }
}
