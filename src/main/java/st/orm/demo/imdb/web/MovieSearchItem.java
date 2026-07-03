package st.orm.demo.imdb.web;

import st.orm.demo.imdb.model.MovieSummary;

public record MovieSearchItem(String id, String title, Integer year) {

    public static MovieSearchItem of(MovieSummary summary) {
        return new MovieSearchItem(summary.id(), summary.primaryTitle(), summary.startYear());
    }
}
