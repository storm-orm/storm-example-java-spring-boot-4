package st.orm.demo.imdb.web;

import java.util.List;

/**
 * One keyset window of results plus the opaque cursor for the next window.
 * The cursor exists for client-server communication: it encodes exactly what
 * the server needs to continue the scroll (key position, window size, and
 * direction). Clients treat it as a black box — never parsed, never
 * constructed — and echo it back unchanged to fetch the adjacent window.
 * Server-side code never needs it: Storm's Window exposes typed
 * next()/previous() Scrollables for direct navigation, and the cursor is
 * merely their serialized form.
 */
public record SearchWindow<T>(List<T> items, String nextCursor) {
}
