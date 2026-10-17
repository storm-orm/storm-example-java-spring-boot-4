package st.orm.demo.imdb.repository;

import java.util.regex.Pattern;
import st.orm.Data;
import st.orm.Metamodel;
import st.orm.SqlFunction;

/** The accent folding and input normalization the searches share. */
final class SearchText {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * The database's accent folding (V1): the text without its accents, through
     * PostgreSQL's unaccent. The searches apply it to the columns they match, and
     * Storm applies it to the input as well, so the database folds both sides and
     * "leon", "Léon" and "LÉON" all find Léon. The trigram indexes of V1
     * are built on LOWER(fold_accents(column)), the expression the searches
     * compare, so they serve them without a column that stores the folded text.
     */
    static final SqlFunction<String> FOLD_ACCENTS = SqlFunction.of("fold_accents");

    private SearchText() {
    }

    /** The column without its accents, as the searches match it: {@code folded(MovieSummary_.primaryTitle)}. */
    static <T extends Data> Metamodel<T, String> folded(Metamodel<T, String> path) {
        return FOLD_ACCENTS.apply(path);
    }

    /**
     * The search input as a title or name would read it, for the ranks that
     * compare it whole: its words one space apart.
     */
    static String searchText(String input) {
        return WHITESPACE.matcher(input.trim()).replaceAll(" ");
    }
}
