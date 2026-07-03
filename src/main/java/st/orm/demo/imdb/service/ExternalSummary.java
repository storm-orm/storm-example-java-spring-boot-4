package st.orm.demo.imdb.service;

/** A short description of a movie or person, fetched from Wikipedia. */
public record ExternalSummary(
        String title,
        String description,
        String extract,
        String url
) {
}
