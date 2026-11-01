package st.orm.demo.imdb.web;

import st.orm.demo.imdb.model.Person;

public record PersonSearchItem(String id, String name) {

    public static PersonSearchItem of(Person person) {
        return new PersonSearchItem(person.id(), person.primaryName());
    }
}
