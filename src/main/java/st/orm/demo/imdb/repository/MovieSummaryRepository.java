package st.orm.demo.imdb.repository;

import static st.orm.Operator.EQUALS;
import static st.orm.Operator.EQUALS_IGNORE_CASE;
import static st.orm.Operator.LESS_THAN_OR_EQUAL;
import static st.orm.Operator.STARTS_WITH_IGNORE_CASE;
import static st.orm.demo.imdb.repository.SearchText.folded;
import static st.orm.demo.imdb.repository.SearchText.searchText;

import java.util.List;
import st.orm.Data;
import st.orm.Scrollable;
import st.orm.Window;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.demo.imdb.model.MovieGenre_;
import st.orm.demo.imdb.model.MovieSummary;
import st.orm.demo.imdb.model.MovieSummary_;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.model.Principal_;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Rating_;
import st.orm.repository.ProjectionRepository;
import st.orm.template.PredicateBuilder;
import st.orm.template.WhereBuilder;

public interface MovieSummaryRepository extends ProjectionRepository<MovieSummary, String> {

    /**
     * Movie search with keyset scrolling. Every word of the query must occur
     * in the title, the original title or the name of a lead (one of the
     * first three billed) or the director, ignoring case and accents, so
     * "leon" finds Léon, "matrix reloaded" finds The Matrix Reloaded,
     * "fabuleux destin" finds Amélie and "matrix keanu" finds both Matrix
     * movies; a quoted phrase counts as one word, and % or _ match
     * themselves. The database removes the accents from the titles, the names
     * and the query alike ({@link SearchText#folded}), through the expression
     * its trigram indexes are built on. The projection keeps the SELECT to the
     * columns the result grid needs.
     * The credits are a semi-join: they filter the movies without adding rows,
     * so each movie comes back once without a DISTINCT, each word may match a
     * different credit, and a movie without credits is still found by its
     * title. The condition on the join limits the credits to leads and
     * directors, which the partial index of V1 holds, so a fragment as common
     * as "rob" reads a few thousand index entries instead of every credit of
     * every Robert.
     * The results are ranked as the suggestions are: a title equal to the
     * query first, a title starting with it next, then the rest. Scroll
     * follows the ranks, so the scrollable orders each rank, by the vote
     * count of the joined rating on the search page ({@link #mostVotedFirst}).
     * The returned Window offers both navigation modes: typed
     * next()/previous() Scrollables for server-side code, and opaque
     * nextCursor()/previousCursor() strings for clients.
     */
    default Window<MovieSummary> searchMovies(String query, Scrollable<MovieSummary> scrollable) {
        String title = searchText(query);
        return select()
                .innerJoin(Rating.class).on(MovieSummary.class)
                .semiJoin(Principal.class).on(MovieSummary.class, MovieSummaryRepository::leadOrDirector)
                .where(it -> it.search(title, folded(MovieSummary_.primaryTitle), folded(MovieSummary_.originalTitle), folded(Principal_.person.primaryName)))
                .orderFirst(it -> titleEquals(it, title))
                .orderFirst(it -> titleStartsWith(it, title))
                .narrow(MovieSummary.class)
                .scroll(scrollable);
    }

    /**
     * Movie suggestions for the search auto-complete, matched and ranked as
     * {@link #searchMovies} matches and ranks them: a title equal to the query
     * first and a title starting with it next, so "napoleon" puts Napoleon
     * above the more popular Napoleon Dynamite, and both above the Leonardo
     * DiCaprio movies "leon" also finds; within each rank the vote count
     * orders them, so well-known movies surface first. The rank is computed
     * over the matched rows only, which the sort reads anyway. The box
     * queries on every keystroke, so it uses suggest rather than search:
     * words of one or two letters, which the trigram indexes cannot serve,
     * are ignored, and nothing is suggested until a word of three letters is
     * typed; "de niro" suggests what "niro" does. Rating's foreign key
     * references the Movie entity; the join onto this projection resolves
     * automatically because both map the same table.
     */
    default List<MovieSummary> findMovieSuggestions(String query, int limit) {
        String title = searchText(query);
        return select()
                .innerJoin(Rating.class).on(MovieSummary.class)
                .semiJoin(Principal.class).on(MovieSummary.class, MovieSummaryRepository::leadOrDirector)
                .where(it -> it.suggest(title, folded(MovieSummary_.primaryTitle), folded(MovieSummary_.originalTitle), folded(Principal_.person.primaryName)))
                .orderFirst(it -> titleEquals(it, title))
                .orderFirst(it -> titleStartsWith(it, title))
                .orderByDescending(Rating_.voteCount)
                .limit(limit)
                .getResultList();
    }

    /**
     * All movies in a genre with keyset scrolling. The junction table has a
     * composite key and cannot be scrolled directly, so the scroll runs on
     * the movie's simple primary key with a JOIN through the junction table,
     * resolved automatically against the projection by table. The scroll key
     * has to identify one row of the projection, which a key on the junction
     * table would not. The rating is joined so the scrollable can order the
     * genre by its vote count, as the browse page does ({@link #mostVotedFirst});
     * every imported movie has one, so the inner join drops none.
     */
    default Window<MovieSummary> scrollByGenre(Genre genre, Scrollable<MovieSummary> scrollable) {
        return select()
                .innerJoin(MovieGenre.class).on(MovieSummary.class)
                .innerJoin(Rating.class).on(MovieSummary.class)
                .where(MovieGenre_.genre, genre)
                .narrow(MovieSummary.class)
                .scroll(scrollable);
    }

    /**
     * Most votes first, then the id, which addresses a row: the order of
     * the search page within each rank and of the browse page. The vote
     * count is a column of the rating {@link #searchMovies} and
     * {@link #scrollByGenre} join.
     */
    static Scrollable<MovieSummary> mostVotedFirst(int size) {
        return Scrollable.of(MovieSummary_.id, size).sortByDescending(Rating_.voteCount);
    }

    /** The title or the original title equals the text, ignoring case. */
    private static PredicateBuilder<Data, ?, ?> titleEquals(WhereBuilder<Data, ?, ?> it, String text) {
        return it.where(folded(MovieSummary_.primaryTitle), EQUALS_IGNORE_CASE, text)
                .or(it.where(folded(MovieSummary_.originalTitle), EQUALS_IGNORE_CASE, text));
    }

    /** The title or the original title starts with the text, ignoring case. */
    private static PredicateBuilder<Data, ?, ?> titleStartsWith(WhereBuilder<Data, ?, ?> it, String text) {
        return it.where(folded(MovieSummary_.primaryTitle), STARTS_WITH_IGNORE_CASE, text)
                .or(it.where(folded(MovieSummary_.originalTitle), STARTS_WITH_IGNORE_CASE, text));
    }

    /**
     * The credits a movie is found by: the leads, billed first to third, and the
     * directors. The partial index of migration V1 holds exactly these rows.
     */
    private static PredicateBuilder<Data, ?, ?> leadOrDirector(WhereBuilder<Data, ?, ?> it) {
        return it.where(Principal_.ordering, LESS_THAN_OR_EQUAL, 3)
                .or(it.where(Principal_.category, EQUALS, "director"));
    }
}
