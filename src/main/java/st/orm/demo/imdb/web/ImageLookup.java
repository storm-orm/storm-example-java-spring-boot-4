package st.orm.demo.imdb.web;

/**
 * What a lookup of an image found: its URL, no image at all, or nothing yet
 * because IMDB could not be asked or did not answer. Only the first two are
 * answers worth keeping.
 */
public sealed interface ImageLookup {

    record Found(String url) implements ImageLookup {
    }

    record None() implements ImageLookup {
    }

    record Failed() implements ImageLookup {
    }
}
