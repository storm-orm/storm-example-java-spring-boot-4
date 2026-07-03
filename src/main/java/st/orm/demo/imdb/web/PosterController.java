package st.orm.demo.imdb.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves movie posters and person photos through IMDB's public suggestion
 * API and redirects to the CDN image — the API accepts both tconst (tt...)
 * and nconst (nm...) identifiers. Serving images through this endpoint
 * avoids CORS and WAF issues that direct client-side calls to IMDB would
 * hit. Results — including "no image" — are cached for the lifetime of the
 * application.
 */
@RestController
public class PosterController {

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Optional<String>> imageUrlsByImdbId = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public PosterController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping({"/api/poster/{imdbId}", "/api/photo/{imdbId}"})
    public ResponseEntity<Void> image(@PathVariable String imdbId) {
        Optional<String> imageUrl = imageUrlsByImdbId.computeIfAbsent(imdbId,
                id -> Optional.ofNullable(fetchImageUrl(id)));
        return redirect(imageUrl);
    }

    /**
     * The suggestion API's video entries carry landscape trailer stills —
     * the closest thing the public API has to a Netflix-style backdrop.
     */
    @GetMapping("/api/backdrop/{imdbId}")
    public ResponseEntity<Void> backdrop(@PathVariable String imdbId) {
        Optional<String> imageUrl = imageUrlsByImdbId.computeIfAbsent("backdrop:" + imdbId,
                key -> Optional.ofNullable(fetchBackdropUrl(imdbId)));
        return redirect(imageUrl);
    }

    private ResponseEntity<Void> redirect(Optional<String> imageUrl) {
        return imageUrl
                .map(url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String fetchBackdropUrl(String imdbId) {
        try {
            URI uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/" + imdbId + ".json?includeVideos=1");
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(uri).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode match = firstMatchingId(objectMapper.readTree(response.body()).path("d"), imdbId);
            if (match == null) {
                return null;
            }
            JsonNode widest = null;
            int widestWidth = -1;
            for (JsonNode video : match.path("v")) {
                int width = video.path("i").path("width").asInt(0);
                if (width > widestWidth) {
                    widestWidth = width;
                    widest = video;
                }
            }
            if (widest == null) {
                return null;
            }
            String imageUrl = widest.path("i").path("imageUrl").asString(null);
            return imageUrl == null ? null : imageUrl.replace("._V1_.", "._V1_SX1280.");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fetchImageUrl(String imdbId) {
        try {
            URI uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/" + imdbId + ".json");
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(uri).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            JsonNode match = firstMatchingId(objectMapper.readTree(response.body()).path("d"), imdbId);
            if (match == null) {
                return null;
            }
            String imageUrl = match.path("i").path("imageUrl").asString(null);
            return imageUrl == null ? null : imageUrl.replace("._V1_.", "._V1_SX400.");
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode firstMatchingId(JsonNode array, String imdbId) {
        for (JsonNode node : array) {
            if (imdbId.equals(node.path("id").asString(""))) {
                return node;
            }
        }
        return null;
    }
}
