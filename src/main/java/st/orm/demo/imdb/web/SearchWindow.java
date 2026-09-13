package st.orm.demo.imdb.web;

import java.util.List;

/**
 * One keyset window of results plus the opaque cursor for the next window.
 * The cursor exists for client-server communication: it carries the position
 * the scroll continues from, under a fingerprint of the ordering, while the
 * ordering and the window size are stated by the request. Clients treat it as
 * a black box — never parsed, never constructed — and echo it back unchanged
 * to fetch the adjacent window. Server-side code never needs it: Storm's
 * Window exposes typed next()/previous() Scrollables for direct navigation.
 */
public record SearchWindow<T>(List<T> items, String nextCursor) {
}
