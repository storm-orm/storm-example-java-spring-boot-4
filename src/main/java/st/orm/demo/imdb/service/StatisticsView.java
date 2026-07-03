package st.orm.demo.imdb.service;

import java.util.List;
import st.orm.demo.imdb.repository.DecadeMovieCount;
import st.orm.demo.imdb.repository.GenreRatingStatistics;
import st.orm.demo.imdb.repository.ProlificActor;

/**
 * Everything the statistics page shows. Serialized — including the Storm
 * entities inside the result types — so the whole view can round-trip
 * through the serialized cache.
 */
public record StatisticsView(
        List<DecadeMovieCount> decades,
        long maxDecadeCount,
        List<GenreRatingStatistics> genreStatistics,
        List<ProlificActor> prolificActors
) {
}
