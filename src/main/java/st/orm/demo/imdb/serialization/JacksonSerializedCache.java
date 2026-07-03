package st.orm.demo.imdb.serialization;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import tools.jackson.databind.ObjectMapper;

/**
 * A Spring Cache that stores its values as serialized JSON — the way an
 * external cache like Redis would. Every hit decodes the payload back into
 * objects, so each read exercises the full serialization round-trip.
 *
 * <p>This works because Storm entities are immutable records: no proxies,
 * no session state, no lazy-loading surprises — what serializes is exactly
 * what was queried, and the decoded copy compares equal to it. The
 * ObjectMapper carries the Storm Jackson module so entity graphs containing
 * Ref fields serialize correctly as well.
 */
public class JacksonSerializedCache extends ConcurrentMapCache {

    private final Class<?> valueType;
    private final ObjectMapper objectMapper;

    public JacksonSerializedCache(String name, Class<?> valueType, ObjectMapper objectMapper) {
        super(name);
        this.valueType = valueType;
        this.objectMapper = objectMapper;
    }

    @Override
    protected Object toStoreValue(Object userValue) {
        return objectMapper.writeValueAsString(userValue);
    }

    @Override
    protected Object fromStoreValue(Object storeValue) {
        return objectMapper.readValue((String) storeValue, valueType);
    }
}
