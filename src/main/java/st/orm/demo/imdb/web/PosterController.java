package st.orm.demo.imdb.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
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
 * hit. An answer — an image, or none — is cached for the lifetime of the
 * application; a failed lookup is not, so the next request asks again
 * rather than showing no image until a restart.
 */
@RestController
public class PosterController {

    private static final Logger logger = LoggerFactory.getLogger(PosterController.class);

    /** How long the browser keeps an answer: a day. */
    private static final CacheControl ANSWER_CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, ImageLookup> lookupsByImdbId = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public PosterController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping({"/api/poster/{imdbId}", "/api/photo/{imdbId}"})
    public ResponseEntity<Void> image(@PathVariable String imdbId) {
        return respondImage(lookup(imdbId, () -> fetch(imdbId, false, entry -> {
            String imageUrl = entry.path("i").path("imageUrl").asString(null);
            return imageUrl == null ? null : imageUrl.replace("._V1_.", "._V1_SX400.");
        })));
    }

    /**
     * The suggestion API's video entries carry landscape trailer stills —
     * the closest thing the public API has to a Netflix-style backdrop.
     */
    @GetMapping("/api/backdrop/{imdbId}")
    public ResponseEntity<Void> backdrop(@PathVariable String imdbId) {
        return respondImage(lookup("backdrop:" + imdbId, () -> fetch(imdbId, true, entry -> {
            JsonNode widest = null;
            int widestWidth = -1;
            for (JsonNode video : entry.path("v")) {
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
        })));
    }

    /**
     * Responds with a 302 redirect to the image, or 404 when there is none; both are answers, which
     * the browser caches for a day, as they are cached here: without it the browser asks again for
     * every image each time a page or the search dropdown shows it. A failed lookup answers 503 and
     * is not cached, so the image is asked for again the next time it is shown.
     */
    private static ResponseEntity<Void> respondImage(ImageLookup lookup) {
        return switch (lookup) {
            case ImageLookup.Found found -> ResponseEntity.status(HttpStatus.FOUND)
                    .cacheControl(ANSWER_CACHE)
                    .location(URI.create(found.url()))
                    .build();
            case ImageLookup.None none -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(ANSWER_CACHE)
                    .build();
            case ImageLookup.Failed failed -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .cacheControl(CacheControl.noStore())
                    .build();
        };
    }

    /** Returns the cached answer for the key, or looks it up, keeping it unless the lookup failed. */
    private ImageLookup lookup(String key, Supplier<ImageLookup> fetch) {
        ImageLookup cached = lookupsByImdbId.get(key);
        if (cached != null) {
            return cached;
        }
        ImageLookup lookup = fetch.get();
        if (!(lookup instanceof ImageLookup.Failed)) {
            lookupsByImdbId.put(key, lookup);
        }
        return lookup;
    }

    private ImageLookup fetch(String imdbId, boolean includeVideos, Function<JsonNode, String> image) {
        try {
            URI uri = URI.create("https://v3.sg.media-imdb.com/suggestion/x/" + imdbId + ".json"
                    + (includeVideos ? "?includeVideos=1" : ""));
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("Image lookup for {} failed: IMDB answered {}.", imdbId, response.statusCode());
                return new ImageLookup.Failed();
            }
            JsonNode match = firstMatchingId(objectMapper.readTree(response.body()).path("d"), imdbId);
            String imageUrl = match == null ? null : image.apply(match);
            return imageUrl == null ? new ImageLookup.None() : new ImageLookup.Found(imageUrl);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("Image lookup for {} failed: {}.", imdbId, e.toString());
            return new ImageLookup.Failed();
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
