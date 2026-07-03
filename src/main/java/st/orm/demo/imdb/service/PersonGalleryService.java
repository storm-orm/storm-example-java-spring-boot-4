package st.orm.demo.imdb.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.PersonGallery;
import st.orm.demo.imdb.model.Photo;
import st.orm.demo.imdb.repository.PersonGalleryRepository;
import st.orm.demo.imdb.repository.PersonRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Photo galleries for the person pages, sourced from the images of the
 * person's Wikipedia article. Unlike the poster and summary enrichment,
 * galleries are not held in memory: the first request fetches the photos
 * from Wikimedia and writes them to the person_gallery table, and every
 * request after that — across restarts and across instances — reads the
 * stored gallery from the database.
 */
@Service
public class PersonGalleryService {

    /** Galleries are only useful in small doses; keep the first dozen photos. */
    private static final int MAX_GALLERY_PHOTOS = 12;

    private static final Logger logger = LoggerFactory.getLogger(PersonGalleryService.class);

    private final PersonRepository personRepository;
    private final PersonGalleryRepository personGalleryRepository;
    private final WikipediaSummaryService wikipediaSummaryService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readOnlyTransaction;
    private final TransactionTemplate transaction;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public PersonGalleryService(PersonRepository personRepository,
                                PersonGalleryRepository personGalleryRepository,
                                WikipediaSummaryService wikipediaSummaryService,
                                ObjectMapper objectMapper,
                                PlatformTransactionManager transactionManager) {
        this.personRepository = personRepository;
        this.personGalleryRepository = personGalleryRepository;
        this.wikipediaSummaryService = wikipediaSummaryService;
        this.objectMapper = objectMapper;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    private record StoredGallery(Person person, PersonGallery gallery) {}

    /**
     * The gallery for a person: the stored photos when present, otherwise
     * freshly fetched from Wikimedia and stored — including an empty result,
     * so an article without usable photos is remembered as such. Transient
     * fetch failures are not stored; a later request simply tries again.
     * Returns null for unknown persons.
     */
    public List<Photo> findGallery(String personId) {
        StoredGallery stored = readOnlyTransaction.execute(status ->
                personRepository.findById(personId)
                        .map(person -> new StoredGallery(person, personGalleryRepository.findById(person).orElse(null)))
                        .orElse(null));
        if (stored == null) {
            return null;
        }
        if (stored.gallery() != null) {
            return stored.gallery().photos();
        }
        // Fetch outside the transaction: no connection is held during HTTP I/O.
        List<Photo> photos = fetchPhotos(stored.person());
        if (photos == null) {
            return List.of();
        }
        transaction.executeWithoutResult(status ->
                personGalleryRepository.upsert(new PersonGallery(stored.person(), photos, Instant.now())));
        return photos;
    }

    private List<Photo> fetchPhotos(Person person) {
        // The summary lookup resolves the person to a canonical article
        // title — it already skips disambiguation pages.
        ExternalSummary article = wikipediaSummaryService.findPersonSummary(person.id());
        if (article == null) {
            return null;
        }
        try {
            String encodedTitle = URLEncoder.encode(article.title().replace(' ', '_'), StandardCharsets.UTF_8);
            URI uri = URI.create("https://en.wikipedia.org/api/rest_v1/page/media-list/" + encodedTitle);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Accept", "application/json")
                    // Wikimedia's API policy requires a descriptive User-Agent.
                    .header("User-Agent", "storm-imdb-demo/1.0 (https://orm.st)")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            List<Photo> photos = new ArrayList<>();
            for (JsonNode item : objectMapper.readTree(response.body()).path("items")) {
                if (!"image".equals(item.path("type").asString("")) || !item.path("showInGallery").asBoolean(false)) {
                    continue;
                }
                Photo photo = toPhoto(item);
                if (photo != null) {
                    photos.add(photo);
                    if (photos.size() == MAX_GALLERY_PHOTOS) {
                        break;
                    }
                }
            }
            return photos;
        } catch (Exception e) {
            // Galleries are optional enrichment: a failed fetch must never
            // break the person page, but it should leave a trace. Nothing is
            // stored, so the next request tries again.
            logger.warn("Fetching the photo gallery for {} ({}) failed", person.primaryName(), person.id(), e);
            return null;
        }
    }

    private Photo toPhoto(JsonNode item) {
        // Actor infoboxes usually carry a signature image — not a photo.
        if (item.path("title").asString("").toLowerCase().contains("signature")) {
            return null;
        }
        JsonNode srcset = item.path("srcset");
        // The srcset is ordered by scale; the last entry is the sharpest.
        if (srcset.isEmpty()) {
            return null;
        }
        String src = srcset.get(srcset.size() - 1).path("src").asString(null);
        if (src == null) {
            return null;
        }
        // Vector graphics are flags, seals and logos — not photos.
        if (src.toLowerCase().contains(".svg")) {
            return null;
        }
        return new Photo(
                src.startsWith("//") ? "https:" + src : src,
                item.path("caption").path("text").asString(null)
        );
    }
}
