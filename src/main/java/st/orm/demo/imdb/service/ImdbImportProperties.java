package st.orm.demo.imdb.service;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "imdb.import")
public record ImdbImportProperties(
        /** Directory where downloaded IMDB dataset files are cached. */
        @DefaultValue("./data") Path cacheDirectory,
        /** Only movies with at least this many votes are imported. */
        @DefaultValue("1000") int minimumVoteCount,
        /** Base URL of the public IMDB dataset files. */
        @DefaultValue("https://datasets.imdbws.com") String datasetBaseUrl
) {
}
