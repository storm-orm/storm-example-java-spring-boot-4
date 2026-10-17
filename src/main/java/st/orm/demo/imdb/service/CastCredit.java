package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.model.Person;

/**
 * A person's credits on one movie, listed once: an actor who also directs
 * holds a credit per role, which the page shows as one card.
 *
 * @param roles the roles in billing order: the character played, or else the job.
 */
public record CastCredit(
        Person person,
        List<String> roles
) {

    /** The roles as the card shows them, separated by a slash. */
    public String role() {
        return String.join(" / ", roles);
    }
}
