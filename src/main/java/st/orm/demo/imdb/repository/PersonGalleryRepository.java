package st.orm.demo.imdb.repository;

import static st.orm.Operator.LESS_THAN;

import java.time.Instant;
import java.util.stream.Stream;
import st.orm.Ref;
import st.orm.Window;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.PersonGallery;
import st.orm.demo.imdb.model.PersonGallery_;
import st.orm.repository.EntityRepository;

/**
 * The entire persistence layer of the gallery feature: findById, upsert and
 * friends all come with EntityRepository.
 */
public interface PersonGalleryRepository extends EntityRepository<PersonGallery, Ref<Person>> {

    /**
     * The galleries last fetched before {@code cutoff}, in windows of
     * {@code windowSize} rows over the key. Each window is one statement that
     * has returned and closed before the loop sees it, so the connection is
     * free while the refresh resolves the persons, calls Wikimedia and writes
     * the window back. A result stream would hold the connection instead and
     * refuse every one of those statements.
     */
    default Stream<Window<PersonGallery>> findStaleGalleries(Instant cutoff, int windowSize) {
        return select()
                .where(PersonGallery_.fetchedAt, LESS_THAN, cutoff)
                .windows(windowSize);
    }
}
