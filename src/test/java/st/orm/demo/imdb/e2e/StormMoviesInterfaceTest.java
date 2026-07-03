package st.orm.demo.imdb.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.ScreenshotAnimations;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Interface tests against the running application (./gradlew bootRun, then
 * ./gradlew e2eTest). They exercise the user-facing behavior: navigation,
 * search auto-complete, infinite scrolling, the watchlist cycle, posters,
 * and overall visual quality.
 */
@Tag("e2e")
class StormMoviesInterfaceTest {

    private static final String baseUrl = System.getProperty("app.baseUrl", "http://localhost:8080");
    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;

    @BeforeEach
    void createPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    /* ── Home page ────────────────────────────────────────────────── */

    @Test
    void homePageShowsSearchFeaturedMovieTop10AndGenreNavigation() {
        page.navigate(baseUrl + "/");

        assertThat(page.locator(".navbar [data-search-input]")).isVisible();
        assertThat(page.locator(".search-hero [data-search-input]")).isVisible();

        // The featured hero is seeded with The Matrix on first launch.
        assertThat(page.locator(".hero-title")).isVisible();
        assertThat(page.locator(".hero .poster")).isVisible();

        assertThat(page.locator("#top-rated-section .movie-card")).hasCount(10);

        Locator genreChips = page.locator(".genre-bar .genre-chip");
        assertTrue(genreChips.count() >= 10, "Expected a well-filled genre bar");
        assertThat(genreChips.first().locator(".count")).hasText(Pattern.compile("[\\d,]+"));
    }

    /* ── Search auto-complete ─────────────────────────────────────── */

    @Test
    void autoCompleteSuggestsMoviesAndNavigatesToTheMoviePage() {
        page.navigate(baseUrl + "/");
        Locator searchInput = page.locator(".navbar [data-search-input]");
        searchInput.pressSequentially("matrix", new Locator.PressSequentiallyOptions().setDelay(60.0));

        Locator movieSuggestion = page.locator(".navbar .suggestion:has(.suggestion-type-movie)").first();
        assertThat(movieSuggestion).isVisible();
        assertThat(movieSuggestion.locator(".suggestion-type")).hasText("Movie");
        assertThat(movieSuggestion.locator(".suggestion-text"))
                .containsText(Pattern.compile("matrix", Pattern.CASE_INSENSITIVE));

        movieSuggestion.click();
        page.waitForURL(Pattern.compile(".*/movie/tt\\d+"));
        assertThat(page.locator(".movie-title")).containsText(Pattern.compile("matrix", Pattern.CASE_INSENSITIVE));
    }

    @Test
    void autoCompleteSuggestsPersonsAndNavigatesToThePersonPage() {
        page.navigate(baseUrl + "/");
        Locator searchInput = page.locator(".navbar [data-search-input]");
        searchInput.pressSequentially("keanu reev", new Locator.PressSequentiallyOptions().setDelay(60.0));

        Locator personSuggestion = page.locator(".navbar .suggestion:has(.suggestion-type-person)").first();
        assertThat(personSuggestion).isVisible();
        assertThat(personSuggestion.locator(".suggestion-type")).hasText("Person");

        personSuggestion.click();
        page.waitForURL(Pattern.compile(".*/person/nm\\d+"));
        assertThat(page.locator(".page-title")).containsText(Pattern.compile("keanu", Pattern.CASE_INSENSITIVE));
    }

    /* ── Search results + infinite scroll ─────────────────────────── */

    @Test
    void searchResultsShowMoviesAndPersonsAndLoadMoreOnScroll() {
        page.navigate(baseUrl + "/search?query=love");

        assertThat(page.locator("#movie-results-section .section-title")).hasText("Movies");
        assertThat(page.locator("#person-results-section .section-title")).hasText("People");
        assertThat(page.locator("#movie-results .movie-card").first()).isVisible();
        assertThat(page.locator("#person-results .person-card").first()).isVisible();

        int initialCount = page.locator("#movie-results .movie-card").count();
        page.locator("#movie-results-section [data-infinite-scroll]").scrollIntoViewIfNeeded();
        // The next keyset window is appended automatically.
        assertThat(page.locator("#movie-results .movie-card").nth(initialCount)).isVisible();
        assertTrue(page.locator("#movie-results .movie-card").count() > initialCount);
    }

    /* ── Browse by genre + infinite scroll ────────────────────────── */

    @Test
    void browsingAGenreLoadsMoreMoviesOnScroll() {
        page.navigate(baseUrl + "/browse/Drama");

        assertThat(page.locator(".page-title")).hasText("Drama");
        assertThat(page.locator(".page-subtitle")).containsText(Pattern.compile("[\\d,]+ movies"));

        int initialCount = page.locator("#browse-results .movie-card").count();
        assertEquals(24, initialCount);

        page.locator("[data-infinite-scroll]").scrollIntoViewIfNeeded();
        assertThat(page.locator("#browse-results .movie-card").nth(initialCount)).isVisible();
        assertTrue(page.locator("#browse-results .movie-card").count() > initialCount);
    }

    /* ── Movie detail ─────────────────────────────────────────────── */

    @Test
    void movieDetailShowsInfoCastAmbientBackdropAndRelatedMovies() {
        page.navigate(baseUrl + "/movie/tt0133093");

        assertThat(page.locator(".movie-title")).hasText("The Matrix");
        assertThat(page.locator(".rating-badge").first()).isVisible();
        assertThat(page.locator(".movie-genres .genre-chip").first()).isVisible();

        // Server renders the blurred poster; the sharp landscape backdrop
        // swaps in asynchronously — either way the style points at our API.
        String backdropStyle = page.locator("#movie-backdrop").getAttribute("style");
        assertTrue(backdropStyle.contains("/api/"), "Backdrop uses an API image");
        assertTrue(page.locator("#movie-backdrop").getAttribute("data-backdrop-source")
                .contains("/api/backdrop/tt0133093"));

        assertTrue(page.locator("#cast-section .credit-card").count() >= 4);
        assertThat(page.locator("#related-section .movie-card").first()).isVisible();

        // The Wikipedia plot summary loads asynchronously.
        assertThat(page.locator("#movie-description"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15_000.0));
        assertTrue(page.locator("#movie-description").textContent().length() > 80);
    }

    /* ── Watchlist ────────────────────────────────────────────────── */

    @Test
    void theWatchlistTogglePersistsAcrossHomeAndWatchlistPages() {
        page.navigate(baseUrl + "/movie/tt0133093");
        Locator toggleButton = page.locator("[data-watchlist-toggle]");
        Locator label = page.locator("[data-watchlist-label]");

        // Make the starting state deterministic: ensure OFF first.
        if (label.textContent().contains("On watchlist")) {
            toggleButton.click();
            assertThat(label).hasText("Add to watchlist");
        }

        toggleButton.click();
        assertThat(label).hasText("On watchlist");

        page.navigate(baseUrl + "/");
        assertThat(page.locator("#watchlist-section")).isVisible();
        assertThat(page.locator("#watchlist-section .movie-card-title").first()).hasText("The Matrix");

        page.navigate(baseUrl + "/watchlist");
        assertThat(page.locator("#watchlist-results .movie-card-title").first()).hasText("The Matrix");

        // Toggle back off to leave no residue.
        page.navigate(baseUrl + "/movie/tt0133093");
        toggleButton.click();
        assertThat(label).hasText("Add to watchlist");
    }

    @Test
    void theWatchlistPagePaginatesOnceItHasMoreThanOnePage() {
        var request = page.context().request();
        List<String> movieIds = fetchSomeMovieIds(13);

        movieIds.forEach(movieId -> request.post(baseUrl + "/api/watchlist/" + movieId));
        try {
            page.navigate(baseUrl + "/watchlist?page=1");
            assertEquals(12, page.locator("#watchlist-results .movie-card").count());
            assertThat(page.locator(".pagination .page-info")).containsText(Pattern.compile("Page 1 of \\d+"));

            page.locator(".pagination a", new Page.LocatorOptions().setHasText("Next")).click();
            page.waitForURL(Pattern.compile(".*page=2.*"));
            assertTrue(page.locator("#watchlist-results .movie-card").count() >= 1);
        } finally {
            // Toggle everything back off.
            movieIds.forEach(movieId -> request.post(baseUrl + "/api/watchlist/" + movieId));
        }
    }

    /* ── Recently viewed ──────────────────────────────────────────── */

    @Test
    void aVisitedMovieAppearsInTheRecentlyViewedSection() {
        // The most recent view becomes the featured hero, so visit two:
        // Shawshank first, then The Godfather — Shawshank lands in the row.
        page.navigate(baseUrl + "/movie/tt0111161");
        page.navigate(baseUrl + "/movie/tt0068646");

        page.navigate(baseUrl + "/");
        assertThat(page.locator(".hero-title")).hasText("The Godfather");
        assertThat(page.locator("#recently-viewed-section")).isVisible();
        assertThat(page.locator("#recently-viewed-section .movie-card-title", new Page.LocatorOptions())
                .filter(new Locator.FilterOptions().setHasText("The Shawshank Redemption")).first()).isVisible();
    }

    /* ── Person detail ────────────────────────────────────────────── */

    @Test
    void clickingACastMemberShowsTheFilmographyWithStatistics() {
        page.navigate(baseUrl + "/movie/tt0133093");
        Locator firstCastMember = page.locator("#cast-section .credit-card").first();
        String castMemberName = firstCastMember.locator(".person-card-name").textContent();
        firstCastMember.click();

        page.waitForURL(Pattern.compile(".*/person/nm\\d+"));
        assertThat(page.locator(".page-title")).hasText(castMemberName);
        assertTrue(page.locator("#person-statistics .stat-chip").count() >= 1);
        assertTrue(page.locator("#filmography-section .ranked-row").count() >= 1);
        assertThat(page.locator("#filmography-section .rating-badge").first()).isVisible();

        // Personal details: the headshot header and the Wikipedia biography.
        assertThat(page.locator(".person-header .avatar-large")).isVisible();
        assertThat(page.locator("#person-bio"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15_000.0));
    }

    /* ── Statistics ───────────────────────────────────────────────── */

    @Test
    void theStatisticsPageRendersAllAggregateSections() {
        page.navigate(baseUrl + "/statistics");

        assertTrue(page.locator("#decades-card .bar-row").count() >= 5);
        assertTrue(page.locator("#genres-card .stat-row").count() >= 5);
        assertEquals(10, page.locator("#actors-card .stat-row").count());
        // Bars are actually scaled: at least one bar is wider than 50%.
        Object widestBar = page.locator("#decades-card .bar-fill")
                .evaluateAll("bars => Math.max(...bars.map(bar => parseFloat(bar.style.width)))");
        assertTrue(((Number) widestBar).doubleValue() > 50.0);
    }

    /* ── Posters ──────────────────────────────────────────────────── */

    @Test
    void thePosterEndpointRedirectsToACdnImageAndThePosterRenders() {
        APIResponse response = page.context().request().get(
                baseUrl + "/api/poster/tt0133093",
                RequestOptions.create().setMaxRedirects(0));
        assertEquals(302, response.status());
        String location = response.headers().getOrDefault("location", "");
        assertTrue(location.startsWith("https://"), "Expected a CDN redirect, got: " + location);

        APIResponse backdropResponse = page.context().request().get(
                baseUrl + "/api/backdrop/tt0133093",
                RequestOptions.create().setMaxRedirects(0));
        assertEquals(302, backdropResponse.status());

        page.navigate(baseUrl + "/movie/tt0133093");
        Locator poster = page.locator(".movie-hero-poster img");
        page.waitForCondition(() -> (Boolean) poster.evaluate("img => img.complete && img.naturalWidth > 0"));
    }

    @Test
    void moviesWithoutPostersShowTheStyledGradientPlaceholder() {
        // Deterministic poster failure: block the poster endpoint entirely.
        page.route("**/api/poster/**", route -> route.abort());
        page.navigate(baseUrl + "/browse/Drama");

        Locator firstPoster = page.locator("#browse-results .movie-card .poster").first();
        assertThat(firstPoster.locator("img")).isHidden();
        assertThat(firstPoster.locator(".poster-placeholder span")).isVisible();
    }

    /* ── UI polish ────────────────────────────────────────────────── */

    @Test
    void pagesAreStyledDoNotOverflowAndScreenshotsAreCaptured() {
        Path screenshots = Paths.get("build/e2e-screenshots");
        List<Map.Entry<String, String>> pages = List.of(
                Map.entry("/", "home"),
                Map.entry("/movie/tt0133093", "movie"),
                Map.entry("/person/nm0000206", "person"),
                Map.entry("/statistics", "statistics")
        );
        for (Map.Entry<String, String> entry : pages) {
            page.navigate(baseUrl + entry.getKey());
            // The stylesheet loaded: the dark theme background is applied.
            Object background = page.evaluate("getComputedStyle(document.body).backgroundColor");
            assertEquals("rgb(11, 13, 20)", background);
            // No horizontal overflow.
            Number overflow = (Number) page.evaluate(
                    "document.documentElement.scrollWidth - document.documentElement.clientWidth");
            assertTrue(overflow.intValue() <= 1, "Horizontal overflow on " + entry.getKey());
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshots.resolve(entry.getValue() + ".png"))
                    .setFullPage(false)
                    // Fast-forward CSS animations so screenshots show settled state.
                    .setAnimations(ScreenshotAnimations.DISABLED));
        }
    }

    /* ── Helpers ──────────────────────────────────────────────────── */

    private List<String> fetchSomeMovieIds(int count) {
        APIResponse response = page.context().request().get(baseUrl + "/api/search/movies?query=the");
        Matcher matcher = Pattern.compile("\"id\":\"(tt\\d+)\"").matcher(response.text());
        List<String> ids = new ArrayList<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        assertTrue(ids.size() >= count, "Expected at least " + count + " movies for the pagination test");
        return ids.stream().limit(count).toList();
    }

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void stopBrowser() {
        browser.close();
        playwright.close();
    }
}
