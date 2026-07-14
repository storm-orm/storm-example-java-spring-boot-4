# Storm Movies · Java + Spring Boot 4 example

An example movie browser built with [Storm ORM](https://orm.st) on Spring Boot 4
and Java 21. It imports the public [IMDB dataset](https://datasets.imdbws.com/)
into PostgreSQL and serves a server-rendered web app (Thymeleaf + a little
vanilla JS) for browsing movies, people, genres, ratings, and a watchlist.

The project exists to show what idiomatic Storm looks like in a real Spring
Boot application: immutable record entities, metamodel-based queries,
Spring-managed transactions, and schema validation. No JPA, no proxies, no
persistence context.

## Stack

- Java 21, Spring Boot 4.1 (WebMVC + Thymeleaf, virtual threads enabled)
- Storm ORM (`storm-spring-boot-starter` + `storm-java21`) with the
  annotation-processor metamodel generator and `RAW` SQL string templates
- PostgreSQL 17 (Docker Compose) with Flyway migrations
- Jackson (`storm-jackson3`) for the JSON APIs and cache values, including
  Storm's `Ref` serialization
- JUnit 5 + `storm-test` on H2 for repository tests, Playwright for interface tests

## Java 21 preview features

Storm's Java API builds SQL with **JDK String Templates** (JEP 430), a
_preview_ feature that shipped in Java 21 and 22 and was then withdrawn. The
project therefore pins the toolchain to **Java 21 specifically** and enables
preview features everywhere: `--enable-preview` is wired into every
`JavaCompile` task, every `Test` task, and `bootRun` in `build.gradle.kts`. The
app must be built and run on a Java 21 JDK; later JDKs no longer have the
feature. The `RAW."... \{expression} ..."` syntax you see in the repositories is
that preview feature in action; the interpolated `\{...}` values are separated
from the SQL fragments at compile time, so the templates are injection-safe by
construction.

String templates were withdrawn for a redesign, not abandoned: Project Amber is
reworking the feature, and a revised proposal is expected to return to the JDK.
Storm deliberately ships String Template support today rather than waiting:
the Java API is production-ready, and its template syntax will track the
redesigned feature as it lands. Once string templates return to the JDK as a
stable feature, the Java API moves front and center alongside Kotlin, without
preview flags or a version pin. Everything else in this example (entities,
repositories, the metamodel, transactions) is stable Java and unaffected by
the preview status.

## Running the application

Prerequisites: JDK 21 and Docker.

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the application
./gradlew bootRun

# 3. Open the app
open http://localhost:8080
```

On first startup the app runs the Flyway migration and imports the IMDB
dataset: movies with at least 1,000 votes (configurable via
`imdb.import.minimum-vote-count`), plus their genres, cast, crew, and ratings.
The dataset files (~1.2 GB) are downloaded once and cached in `./data`, then
streamed through Storm's batch inserts, so expect the first startup to take a
few minutes. The import is skipped entirely on subsequent startups once movie data
is present.

To start over with an empty database:

```bash
docker compose down -v
```

Movie posters, person photos, and plot summaries are fetched at runtime from
the IMDB suggestion API and the Wikipedia REST API, so the app looks best with
internet access.

## Project layout

```
src/main/java/st/orm/demo/imdb/
├── model/          Storm entities (@PK, @FK) and projections, as records
├── repository/     EntityRepository interfaces with QueryBuilder queries
├── service/        Business logic in Storm transaction(...) blocks,
│                   plus the streaming IMDB importer
├── web/            MVC controllers (pages) and REST controllers (/api/**)
└── serialization/  Jackson support: custom serializers and the
                    JSON-serialized Spring cache
src/main/resources/
├── db/migration/   Flyway schema (V1__create_schema.sql)
├── templates/      Thymeleaf views
└── static/         CSS, JS, images
```

## What to look at

Each part of the app demonstrates a Storm feature:

- **Entities** (`model/`): immutable records with `@PK`, `@FK`, `@UK`, and
  composite keys (`MovieGenre`, `Principal`). `MovieView` is a `Ref`-backed
  entity; `MovieSummary` / `PersonSummary` are database-view-style projections
  that select a subset of columns.
- **Repositories** (`repository/`): `EntityRepository` interfaces with default
  methods using the type-safe QueryBuilder and generated metamodel
  (`Movie_.startYear`, `Principal_.person`). Aggregations return plain records;
  computed expressions use `RAW` SQL string templates with metamodel references.
- **Transactions** (`service/`): Storm's programmatic `transaction(...)` API
  at the service level, with `TransactionOptions` for the read-only request
  boundaries. `MovieService.viewMovie` is the one declarative `@Transactional`
  example: both run through the same Spring transaction manager, so they
  compose freely. The gallery service keeps Spring's programmatic
  `TransactionTemplate` for its split fetch/store boundary.
- **Observability** (`application.yaml`): every query and transaction is
  reported as a Micrometer Observation (`storm.query`, `storm.transaction`),
  surfaced by Actuator at `/actuator/metrics/storm.query`. Query observations
  follow the OpenTelemetry database semantic conventions, and the trace
  context rides along as a SQL comment on every statement.
- **Streaming import** (`service/ImdbDataImporter.java`): a `Stream`-based
  pipeline that parses TSV rows into entities and hands them to Storm's batch
  insert, one pass per file, without materializing entity lists.
- **Schema validation**: on by default. The starter verifies every entity
  against the live database schema at startup;
  `EntitySchemaValidationTest` does the same in the test suite.
- **Serialization** (`serialization/`, `web/` API models): Storm entities
  serialized with Jackson for the REST endpoints (`Ref` fields via
  `storm-jackson3`, `BigDecimal` and `Instant` via field serializers), and a
  Spring cache that stores values as serialized JSON to prove entities survive
  the round-trip (`CacheConfiguration.java`).

## Testing

```bash
./gradlew test
```

Repository tests run on an in-memory H2 database via `@StormTest`, so no
Docker is required. Tests receive an `ORMTemplate` and a `SqlCapture` as parameters, so
they can assert on the SQL Storm generates.

The Playwright interface tests run against a live application:

```bash
./gradlew installPlaywrightBrowsers   # once
./gradlew bootRun                     # in one terminal
./gradlew e2eTest                     # in another
```

## Configuration

Everything lives in `src/main/resources/application.yaml`. The defaults match
the Compose file (database `imdb`, user/password `storm` on `localhost:5432`).
Import behavior is tunable under `imdb.import` (cache directory, minimum vote
count, dataset base URL).
