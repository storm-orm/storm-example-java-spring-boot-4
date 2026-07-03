package st.orm.demo.imdb.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.repository.GenreRepository;
import st.orm.demo.imdb.repository.RatingRepository;
import st.orm.demo.imdb.repository.TopMoviesSort;

@Service
public class TopMoviesService {

    private static final int MINIMUM_VOTE_COUNT = 25_000;
    private static final int TOP_MOVIES_LIMIT = 50;

    private final GenreRepository genreRepository;
    private final RatingRepository ratingRepository;

    public TopMoviesService(GenreRepository genreRepository, RatingRepository ratingRepository) {
        this.genreRepository = genreRepository;
        this.ratingRepository = ratingRepository;
    }

    /** The top movies page: filter options and entries in one read-only transaction. */
    @Transactional(readOnly = true)
    public TopMoviesView findTopMovies(String genreName, TopMoviesSort sortBy) {
        Genre selectedGenre = genreName != null ? genreRepository.findByName(genreName).orElse(null) : null;
        return new TopMoviesView(
                genreRepository.findAllOrderedByName(),
                selectedGenre,
                ratingRepository.findTopMovies(selectedGenre, sortBy, MINIMUM_VOTE_COUNT, TOP_MOVIES_LIMIT)
        );
    }
}
