package st.orm.demo.imdb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import st.orm.demo.imdb.TestSupport;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.PersonGallery;
import st.orm.demo.imdb.model.Photo;
import st.orm.template.ORMTemplate;
import st.orm.test.CapturedSql.Operation;
import st.orm.test.SqlCapture;
import st.orm.test.StormTest;

@StormTest(scripts = {"/schema.sql", "/data.sql"})
class PersonGalleryRepositoryTest {

    @Test
    void aGalleryRoundTripsItsPhotosThroughTheJsonColumn(ORMTemplate orm, SqlCapture capture) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        PersonGalleryRepository galleryRepository = orm.repository(PersonGalleryRepository.class);
        Person keanu = personRepository.getById("nm0000206");
        List<Photo> photos = List.of(
                new Photo("https://upload.wikimedia.org/keanu-1.jpg", "Keanu Reeves in 2019"),
                new Photo("https://upload.wikimedia.org/keanu-2.jpg")
        );

        capture.run(() -> {
            galleryRepository.insert(new PersonGallery(keanu, photos, Instant.parse("2026-07-03T10:00:00Z")));
            assertEquals(photos, galleryRepository.getById(keanu).photos());
        });
        TestSupport.printStatements(capture, "galleryRoundTrip");
        assertEquals(1, capture.count(Operation.INSERT));
    }

    @Test
    void aRefreshedGalleryReplacesTheStoredPhotos(ORMTemplate orm) {
        PersonRepository personRepository = orm.repository(PersonRepository.class);
        PersonGalleryRepository galleryRepository = orm.repository(PersonGalleryRepository.class);
        // Morgan Freeman is not touched by other tests in this class — the
        // @StormTest database is shared across the class's test methods.
        Person morgan = personRepository.getById("nm0000151");

        // The service refreshes with upsert; on Storm <= 1.11.7 H2 cannot
        // infer the parameter types of the MERGE that upsert generates, so
        // the refresh is exercised as insert + update. Fixed upstream — switch
        // both calls to upsert once the next Storm release is in.
        galleryRepository.insert(new PersonGallery(
                morgan,
                List.of(new Photo("https://upload.wikimedia.org/morgan-1.jpg")),
                Instant.parse("2026-07-03T10:00:00Z")));
        galleryRepository.update(new PersonGallery(
                morgan,
                List.of(
                        new Photo("https://upload.wikimedia.org/morgan-2.jpg", "Morgan Freeman in 2018"),
                        new Photo("https://upload.wikimedia.org/morgan-3.jpg")
                ),
                Instant.parse("2026-07-03T11:00:00Z")));

        PersonGallery stored = galleryRepository.getById(morgan);
        assertEquals(2, stored.photos().size());
        assertEquals("https://upload.wikimedia.org/morgan-2.jpg", stored.photos().get(0).url());
        assertEquals(Instant.parse("2026-07-03T11:00:00Z"), stored.fetchedAt());
    }
}
