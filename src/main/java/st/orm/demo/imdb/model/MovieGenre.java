package st.orm.demo.imdb.model;

import st.orm.Entity;
import st.orm.FK;
import st.orm.GenerationStrategy;
import st.orm.PK;
import st.orm.Persist;

/**
 * Junction between movies and genres. Both FK columns are part of the
 * composite primary key, so the entity fields exist purely as join metadata
 * and are excluded from inserts and updates.
 */
public record MovieGenre(
        @PK(generation = GenerationStrategy.NONE) MovieGenrePk id,
        @FK @Persist(insertable = false, updatable = false) Movie movie,
        @FK @Persist(insertable = false, updatable = false) Genre genre
) implements Entity<MovieGenrePk> {

    public MovieGenre(Movie movie, Genre genre) {
        this(new MovieGenrePk(movie.id(), genre.id()), movie, genre);
    }
}
