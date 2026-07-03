package st.orm.demo.imdb;

import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import st.orm.demo.imdb.serialization.JacksonSerializedCache;
import st.orm.demo.imdb.service.StatisticsService;
import st.orm.demo.imdb.service.StatisticsView;
import st.orm.jackson.StormModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring cache backed by serialized JSON values. An in-memory cache would
 * work with plain object references, but serializing every value — like an
 * external store such as Redis would — demonstrates that Storm's immutable
 * entities survive the round-trip unchanged.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new StormModule()).build();
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new JacksonSerializedCache(StatisticsService.STATISTICS_CACHE, StatisticsView.class, objectMapper)
        ));
        return cacheManager;
    }
}
