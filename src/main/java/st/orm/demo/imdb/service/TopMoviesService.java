package st.orm.demo.imdb.service;

import static st.orm.template.Transactions.transaction;

import st.orm.TransactionOptions;

import org.springframework.stereotype.Service;
import st.orm.demo.imdb.model.Genre;
import st.orm.demo.imdb.repository.GenreRepository;
import st.orm.demo.imdb.repository.RatingRepository;
import st.orm.demo.imdb.repository.TopMoviesSort;

@Service
public class TopMoviesService {

    /** Read-only Storm transaction: one consistent snapshot across the queries of a request. */
    private static final TransactionOptions READ_ONLY = TransactionOptions.defaults().withReadOnly(true);

    private static final int MINIMUM_VOTE_COUNT = 25_000;
    private static final int TOP_MOVIES_LIMIT = 50;

    private final GenreRepository genreRepository;
    private final RatingRepository ratingRepository;

    public TopMoviesService(GenreRepository genreRepository, RatingRepository ratingRepository) {
        this.genreRepository = genreRepository;
        this.ratingRepository = ratingRepository;
    }

    /** The top movies page: filter options and entries in one read-only transaction. */
    public TopMoviesView findTopMovies(String genreName, TopMoviesSort sortBy) {
        return transaction(READ_ONLY, tx -> {
            Genre selectedGenre = genreName != null ? genreRepository.findByName(genreName).orElse(null) : null;
            return new TopMoviesView(
                    genreRepository.findAllOrderedByName(),
                    selectedGenre,
                    ratingRepository.findTopMovies(selectedGenre, sortBy, MINIMUM_VOTE_COUNT, TOP_MOVIES_LIMIT)
            );
            });
    }
}
