package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;

import java.util.List;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieGenre_;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieSummary_;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Rating_;
import st.orm.repository.ProjectionRepository;

public interface MovieSummaryRepository extends ProjectionRepository<MovieSummary, String> {

    /**
     * Case-insensitive title search with keyset scrolling. The projection
     * keeps the SELECT to the three columns the result grid actually shows.
     * The returned Window offers both navigation modes: typed
     * next()/previous() Scrollables for server-side code, and opaque
     * nextCursor()/previousCursor() strings for clients.
     */
    default Window<MovieSummary> searchByTitle(String query, Scrollable<MovieSummary> scrollable) {
        String pattern = "%" + query + "%";
        return select()
                .where(RAW."LOWER(\{MovieSummary_.primaryTitle}) LIKE LOWER(\{pattern})")
                .scroll(scrollable);
    }

    /**
     * Title suggestions for the search auto-complete, ranked by popularity
     * (vote count) so well-known movies surface first. Rating's foreign key
     * references the Movie entity; the join onto this projection resolves
     * automatically because both map the same table.
     */
    default List<MovieSummary> findTitleSuggestions(String query, int limit) {
        String pattern = "%" + query + "%";
        return select()
                .innerJoin(Rating.class).on(MovieSummary.class)
                .where(RAW."LOWER(\{MovieSummary_.primaryTitle}) LIKE LOWER(\{pattern})")
                .orderByDescendingAny(Rating_.voteCount)
                .limit(limit)
                .getResultList();
    }

    /**
     * All movies in a genre with keyset scrolling. The junction table has a
     * composite key and cannot be scrolled directly, so the scroll runs on
     * the movie's simple primary key with a JOIN through the junction table,
     * resolved automatically against the projection by table.
     */
    default Window<MovieSummary> scrollByGenre(Genre genre, Scrollable<MovieSummary> scrollable) {
        return select()
                .innerJoin(MovieGenre.class).on(MovieSummary.class)
                .whereAny(predicate -> predicate.whereAny(MovieGenre_.genre, genre))
                .scroll(scrollable);
    }
}
