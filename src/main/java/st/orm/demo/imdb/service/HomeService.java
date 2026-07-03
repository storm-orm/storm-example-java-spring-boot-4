package st.orm.demo.imdb.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.orm.Ref;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.repository.GenreRepository;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.MovieViewRepository;
import st.orm.demo.imdb.repository.RatingRepository;
import st.orm.demo.imdb.repository.WatchlistRepository;

@Service
public class HomeService {

    private static final int RECENT_VIEW_SAMPLE_SIZE = 50;
    private static final int RECENTLY_VIEWED_LIMIT = 12;
    private static final int TOP_RATED_LIMIT = 10;
    private static final int TOP_RATED_MINIMUM_VOTES = 25_000;
    private static final int WATCHLIST_PREVIEW_LIMIT = 12;

    private final MovieRepository movieRepository;
    private final MovieViewRepository movieViewRepository;
    private final RatingRepository ratingRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final WatchlistRepository watchlistRepository;

    public HomeService(MovieRepository movieRepository,
                       MovieViewRepository movieViewRepository,
                       RatingRepository ratingRepository,
                       GenreRepository genreRepository,
                       MovieGenreRepository movieGenreRepository,
                       WatchlistRepository watchlistRepository) {
        this.movieRepository = movieRepository;
        this.movieViewRepository = movieViewRepository;
        this.ratingRepository = ratingRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /** All home page sections, read in one read-only transaction. */
    @Transactional(readOnly = true)
    public HomeView buildHomeView() {
        // Views hold movie refs; dedupe the refs and fetch the movies once.
        List<Ref<Movie>> recentMovieRefs = movieViewRepository.findRecentViews(RECENT_VIEW_SAMPLE_SIZE).stream()
                .map(MovieView::movie)
                .distinct()
                .limit(RECENTLY_VIEWED_LIMIT + 1)
                .toList();
        Map<Ref<Movie>, Movie> moviesByRef = movieRepository.findAllByRef(recentMovieRefs).stream()
                .collect(Collectors.toMap(Ref::of, movie -> movie));
        List<Movie> recentMovies = recentMovieRefs.stream()
                .map(moviesByRef::get)
                .filter(Objects::nonNull)
                .toList();

        FeaturedMovie featured = recentMovies.isEmpty() ? null : featuredMovie(recentMovies.get(0));
        return new HomeView(
                featured,
                recentMovies.stream().skip(1).limit(RECENTLY_VIEWED_LIMIT).toList(),
                ratingRepository.findTopRated(TOP_RATED_MINIMUM_VOTES, TOP_RATED_LIMIT),
                genreRepository.findGenresWithMovieCounts(),
                watchlistRepository.findMostRecent(WATCHLIST_PREVIEW_LIMIT)
        );
    }

    private FeaturedMovie featuredMovie(Movie movie) {
        return new FeaturedMovie(
                movie,
                ratingRepository.findById(movie).orElse(null),
                movieGenreRepository.findGenres(movie)
        );
    }
}
