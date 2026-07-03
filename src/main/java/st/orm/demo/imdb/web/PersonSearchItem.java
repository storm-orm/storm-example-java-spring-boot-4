package st.orm.demo.imdb.web;

import st.orm.demo.imdb.model.PersonSummary;

public record PersonSearchItem(String id, String name) {

    public static PersonSearchItem of(PersonSummary summary) {
        return new PersonSearchItem(summary.id(), summary.primaryName());
    }
}
