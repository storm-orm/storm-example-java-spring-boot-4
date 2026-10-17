package st.orm.demo.imdb.repository;

import st.orm.Ref;
import st.orm.demo.imdb.model.Person;

/**
 * Query result shape: the number of acting credits per person, keyed by the
 * credit's foreign key, so the count needs no join to the person table.
 */
public record ActingCreditCount(
        Ref<Person> person,
        long creditCount
) {
}
