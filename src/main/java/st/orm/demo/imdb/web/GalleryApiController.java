package st.orm.demo.imdb.web;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import st.orm.demo.imdb.model.Photo;
import st.orm.demo.imdb.service.PersonGalleryService;

/**
 * Photo galleries for the person detail pages. The page fetches the gallery
 * asynchronously and renders fine without it.
 */
@RestController
public class GalleryApiController {

    private final PersonGalleryService personGalleryService;

    public GalleryApiController(PersonGalleryService personGalleryService) {
        this.personGalleryService = personGalleryService;
    }

    @GetMapping("/api/gallery/person/{personId}")
    public ResponseEntity<List<Photo>> personGallery(@PathVariable String personId) {
        List<Photo> photos = personGalleryService.findGallery(personId);
        return photos != null ? ResponseEntity.ok(photos) : ResponseEntity.notFound().build();
    }
}
