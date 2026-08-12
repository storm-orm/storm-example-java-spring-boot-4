package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;
import static st.orm.Operator.IN;
import static st.orm.Operator.NOT_EQUALS;

import java.util.List;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.Movie_;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Person_;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.model.PrincipalPk;
import st.orm.demo.imdb.model.Principal_;
import st.orm.demo.imdb.model.Rating;
import st.orm.demo.imdb.model.Rating_;
import st.orm.repository.EntityRepository;

public interface PrincipalRepository extends EntityRepository<Principal, PrincipalPk> {

    /** The cast and crew of a movie in IMDB billing order. */
    default List<Principal> findCast(Movie movie) {
        return select()
                .where(Principal_.movie, movie)
                .orderBy(Principal_.ordering)
                .getResultList();
    }

    /**
     * A person's filmography sorted by rating, best first. The credit holds
     * the movie as a Ref, so the movie is selected alongside it: the join it
     * needs is the one the rating join already asks for.
     */
    default List<FilmographyEntry> findFilmography(Person person) {
        return select(FilmographyEntry.class, RAW."\{Principal.class}, \{Movie.class}, \{Rating_.averageRating}")
                .innerJoin(Rating.class).on(Movie.class)
                .where(Principal_.person, person)
                .orderByDescending(Rating_.averageRating)
                .getResultList();
    }

    /** Movie count and average rating across a person's filmography. */
    default PersonStatistics findStatistics(Person person) {
        return select(PersonStatistics.class, RAW."COUNT(*), AVG(\{Rating_.averageRating})")
                .innerJoin(Rating.class).on(Movie.class)
                .where(Principal_.person, person)
                .getSingleResult();
    }

    /**
     * Movies that share cast members with a given movie, ranked by how many
     * cast members they share. The caller passes the cast it already loaded,
     * which turns a self-join into a straightforward aggregation.
     */
    default List<RelatedMovie> findMoviesSharingCast(List<Person> castMembers, Movie excludedMovie, int limit) {
        return select(RelatedMovie.class, RAW."\{Movie.class}, COUNT(*)")
                .where(predicate -> predicate.where(Principal_.person, IN, castMembers)
                        .and(predicate.where(Principal_.movie, NOT_EQUALS, excludedMovie)))
                .widen()
                .groupBy(Movie_.id, Movie_.primaryTitle, Movie_.originalTitle, Movie_.startYear, Movie_.runtimeMinutes)
                .orderByDescending(RAW."COUNT(*)")
                .limit(limit)
                .getResultList();
    }

    /** Most prolific actors for the statistics page: COUNT + ORDER BY. */
    default List<ProlificActor> findMostProlificActors(int limit) {
        return select(ProlificActor.class, RAW."\{Person.class}, COUNT(*)")
                .where(Principal_.category, IN, List.of("actor", "actress"))
                .widen()
                .groupBy(Person_.id, Person_.primaryName, Person_.birthYear, Person_.deathYear)
                .orderByDescending(RAW."COUNT(*)")
                .limit(limit)
                .getResultList();
    }
}
