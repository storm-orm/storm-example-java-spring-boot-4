package st.orm.demo.imdb.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.orm.demo.imdb.model.Movie;
import st.orm.demo.imdb.model.MovieView;
import st.orm.demo.imdb.model.Person;
import st.orm.demo.imdb.model.Principal;
import st.orm.demo.imdb.repository.MovieGenreRepository;
import st.orm.demo.imdb.repository.MovieRepository;
import st.orm.demo.imdb.repository.MovieViewRepository;
import st.orm.demo.imdb.repository.PrincipalRepository;
import st.orm.demo.imdb.repository.RatingRepository;
import st.orm.demo.imdb.repository.RelatedMovie;
import st.orm.demo.imdb.repository.WatchlistRepository;

@Service
public class MovieService {

    private static final int RELATED_MOVIES_LIMIT = 6;

    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final PrincipalRepository principalRepository;
    private final MovieViewRepository movieViewRepository;
    private final WatchlistRepository watchlistRepository;

    public MovieService(MovieRepository movieRepository,
                        RatingRepository ratingRepository,
                        MovieGenreRepository movieGenreRepository,
                        PrincipalRepository principalRepository,
                        MovieViewRepository movieViewRepository,
                        WatchlistRepository watchlistRepository) {
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.principalRepository = principalRepository;
        this.movieViewRepository = movieViewRepository;
        this.watchlistRepository = watchlistRepository;
    }

    /**
     * Loads the movie detail page and records the visit, so the movie shows
     * up in the recently-viewed section on the home page. The view insert
     * and the page reads share one transaction.
     */
    @Transactional
    public MovieDetail viewMovie(String movieId) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) {
            return null;
        }
        movieViewRepository.insert(new MovieView(movie, Instant.now()));

        List<Principal> cast = principalRepository.findCast(movie);
        List<Person> castMembers = cast.stream().map(Principal::person).distinct().toList();
        List<RelatedMovie> relatedMovies = castMembers.isEmpty()
                ? List.of()
                : principalRepository.findMoviesSharingCast(castMembers, movie, RELATED_MOVIES_LIMIT);
        return new MovieDetail(
                movie,
                ratingRepository.findById(movie).orElse(null),
                movieGenreRepository.findGenres(movie),
                cast,
                relatedMovies,
                watchlistRepository.existsById(movie)
        );
    }
}
