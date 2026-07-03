package st.orm.demo.imdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import st.orm.Ref;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.MovieViewRepository;
import st.orm.jackson.StormModule;
import st.orm.template.ORMTemplate;
import st.orm.test.StormTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Demonstrates Ref serialization: the Jackson StormModule handles the Ref
 * field on MovieView — an unloaded ref serializes as the raw primary key, a
 * loaded ref as the embedded entity — and both round-trip losslessly.
 */
@StormTest(scripts = {"/schema.sql", "/data.sql"})
class MovieViewSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().addModule(new StormModule()).build();

    @Test
    void aViewWithAnUnloadedRefSerializesAsTheRawMovieId(ORMTemplate orm) {
        MovieViewRepository movieViewRepository = orm.repository(MovieViewRepository.class);
        // Views load their movie as an unloaded Ref — just the id.
        MovieView recentView = movieViewRepository.findRecentViews(1).get(0);

        String payload = objectMapper.writeValueAsString(recentView);
        assertTrue(payload.contains("\"movie\":\"tt0133093\""), () -> "Unloaded ref is the raw PK: " + payload);

        MovieView decoded = objectMapper.readValue(payload, MovieView.class);
        assertEquals(recentView, decoded);
    }

    @Test
    void aViewWithALoadedRefSerializesTheEmbeddedMovieEntity(ORMTemplate orm) {
        MovieRepository movieRepository = orm.repository(MovieRepository.class);
        Movie theMatrix = movieRepository.getById("tt0133093");
        MovieView view = new MovieView(42L, Ref.of(theMatrix), Instant.parse("2026-07-01T10:00:00Z"));

        String payload = objectMapper.writeValueAsString(view);
        assertTrue(payload.contains("\"@entity\""), () -> "Loaded ref embeds the entity: " + payload);
        assertTrue(payload.contains("The Matrix"));

        MovieView decoded = objectMapper.readValue(payload, MovieView.class);
        assertEquals(view, decoded);
    }
}
