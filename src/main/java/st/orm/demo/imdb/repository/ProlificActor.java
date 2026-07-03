package st.orm.demo.imdb.repository;

import st.orm.demo.imdb.model.Person;

/**
 * Query result shape: an actor or actress with the number of movies they
 * appeared in. Not backed by a database table or view, so it is a plain
 * record — deliberately not a Data type.
 */
public record ProlificActor(
        Person person,
        long movieCount
) {
}
