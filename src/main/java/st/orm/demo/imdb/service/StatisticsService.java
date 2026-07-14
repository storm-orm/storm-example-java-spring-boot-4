package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import st.orm.demo.imdb.repository.DecadeMovieCount;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.PrincipalRepository;

@Service
public class StatisticsService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    public static final String STATISTICS_CACHE = "statistics";
    private static final int GENRE_MINIMUM_MOVIE_COUNT = 50;
    private static final int GENRE_LIMIT = 10;
    private static final int ACTOR_LIMIT = 10;

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PrincipalRepository principalRepository;

    public StatisticsService(MovieRepository movieRepository,
                             MovieGenreRepository movieGenreRepository,
                             PrincipalRepository principalRepository) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.principalRepository = principalRepository;
    }

    /**
     * All aggregate sections, cached with Spring's cache abstraction. The
     * backing cache stores the view as serialized JSON (see
     * CacheConfiguration) — safe because Storm entities are immutable.
     */
    @Cacheable(STATISTICS_CACHE)
    public StatisticsView buildStatisticsView() {
        return transaction(READ_ONLY, tx -> {
            List<DecadeMovieCount> decades = movieRepository.countMoviesPerDecade();
            long maxDecadeCount = decades.stream().mapToLong(DecadeMovieCount::movieCount).max().orElse(1L);
            return new StatisticsView(
                    decades,
                    maxDecadeCount,
                    movieGenreRepository.findGenreRatingStatistics(GENRE_MINIMUM_MOVIE_COUNT, GENRE_LIMIT),
                    principalRepository.findMostProlificActors(ACTOR_LIMIT)
            );
            });
    }
}
