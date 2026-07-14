package st.orm.demo.imdb.model;


public record PrincipalPk(
        String movieId,
        int ordering
) {
}
