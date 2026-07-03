package st.orm.demo.imdb.repository;

import static java.lang.StringTemplate.RAW;

import java.util.List;
import java.util.Optional;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.model.Genre_;
import st.orm.demo.imdb.model.MovieGenre;
import st.orm.repository.EntityRepository;

public interface GenreRepository extends EntityRepository<Genre, Integer> {

    /** Lookup by the unique genre name (a type-safe @UK key lookup). */
    default Optional<Genre> findByName(String name) {
        return findBy(Genre_.name, name);
    }

    default List<Genre> findAllOrderedByName() {
        return select().orderBy(Genre_.name).getResultList();
    }

    /**
     * All genres with their movie counts for the genre navigation bar.
     * COUNT(*) is a computed expression, so the SELECT clause uses a
     * template; the join, grouping, and ordering stay in code.
     */
    default List<GenreMovieCount> findGenresWithMovieCounts() {
        return select(GenreMovieCount.class, RAW."\{Genre.class}, COUNT(*)")
                .innerJoin(MovieGenre.class).on(Genre.class)
                .groupBy(Genre_.id, Genre_.name)
                .orderBy(Genre_.name)
                .getResultList();
    }
}
