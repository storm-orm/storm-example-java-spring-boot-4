package st.orm.demo.imdb;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Functions the H2 test schema declares, standing in for PostgreSQL's. */
public final class H2Functions {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private H2Functions() {
    }

    /** PostgreSQL's fold_accents (V1) over unaccent, for the Latin accents the tests use. */
    public static String foldAccents(String text) {
        return text == null ? null : COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    }
}
