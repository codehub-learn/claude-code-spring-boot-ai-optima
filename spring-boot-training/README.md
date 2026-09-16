# spring-boot-training

Training module for the Spring Boot fundamentals curriculum (`docs/spring-boot-scope.md`). It
grows one thematic unit at a time; each topic leaves runnable, tested code plus notes here.

Run it:

```bash
./mvnw -pl spring-boot-training spring-boot:run
# add arguments, e.g. the auto-configuration report:
./mvnw -pl spring-boot-training spring-boot:run -Dspring-boot.run.arguments=--debug
```

---

## U1 — Development Environment & Project Setup

### 1.1 Spring Boot introduction

**`@SpringBootApplication`** on `TrainingApplication` is a meta-annotation that combines three:

| Annotation                 | Effect                                                 |
|----------------------------|--------------------------------------------------------|
| `@SpringBootConfiguration` | marks the class as the primary `@Configuration` source |
| `@EnableAutoConfiguration` | activates condition-driven auto-configuration          |
| `@ComponentScan`           | scans this package (`boot`) and below                  |

**Starters** are curated dependency aggregates; versions come from the Spring Boot BOM in the
reactor parent, so the module pom lists starters without versions:

| Starter                           | Pulls in                                                                                                |
|-----------------------------------|---------------------------------------------------------------------------------------------------------|
| `spring-boot-starter-webmvc`      | Spring MVC, embedded Tomcat, Jackson 3 (Boot 4 name; `spring-boot-starter-web` is the deprecated alias) |
| `spring-boot-starter-actuator`    | production endpoints (`health`, `info`, ...)                                                            |
| `spring-boot-starter-test` (test) | JUnit 5, AssertJ, Mockito, Spring test, `MockMvcTester`                                                 |

**Auto-configuration** is driven by `@Conditional*` checks (classpath, existing beans,
properties). Two ways to inspect what matched:

- start with `--debug` — prints the *auto-configuration report* (positive matches, negative
  matches, exclusions) to the console;
- hit `GET /actuator/conditions` at runtime for the same data as JSON.

**`SpringApplication` customization.** `main` builds a `SpringApplication` instance instead of
calling the static `SpringApplication.run(...)`, so the hooks are visible:

- `setBannerMode(Banner.Mode.CONSOLE)` — `CONSOLE` (stdout), `LOG` (through SLF4J), or `OFF`;
- `addListeners(new StartupListener())` — a bootstrap listener, distinct from a bean
  `@EventListener`; `StartupListener` logs a summary on `ApplicationReadyEvent`.

**Custom banner.** `src/main/resources/banner.txt` is picked up automatically. It uses
`${AnsiColor.*}` for colour, `${spring-boot.formatted-version}` for the framework version, and
Maven-filtered `@project.version@` for the app version (see 1.2 for why it is filtered rather
than Boot's `${application.version}` token).

**Actuator `health` / `info`.** `application.yml` exposes only `health,info` over HTTP,
sets `health.show-details: always`, and enables the env `info` contributor so the static
`info.app.*` tree surfaces at `GET /actuator/info`. Build and git metadata are wired in 1.2.

**Virtual threads.** `spring.threads.virtual.enabled: true` makes request handling and
`@Async` run on JDK virtual threads.

### 1.2 Maven

The module `pom.xml` is the teaching artifact here; read it top to bottom.

**Reactor and wrapper.** The repo is a multi-module Maven reactor: a root parent pom (`packaging=pom`) plus this module. Always build
through the wrapper (`./mvnw`,
`.\mvnw.cmd` on Windows) so everyone uses the pinned Maven version (`maven.version` in the parent, enforced by `maven-enforcer-plugin`).
Build just this
module with `-pl spring-boot-training`, and its upstream modules too with `-am`.

**Starters and version management.** Starters are curated dependency aggregates and are
declared without a `<version>`. The reactor parent is `spring-boot-starter-parent`, which
imports the Spring Boot BOM; that BOM pins every managed coordinate (and the versions of
common plugins, including `spring-boot-maven-plugin` and `git-commit-id-maven-plugin`).
Only unmanaged things (`mapstruct`, `jacoco`) carry an explicit `*.version` property in the
parent.

**`spring-boot:run`** launches the app in place, no packaging step:

```bash
./mvnw -pl spring-boot-training spring-boot:run
# pass program arguments (auto-config report):
./mvnw -pl spring-boot-training spring-boot:run -Dspring-boot.run.arguments=--debug
# override the active Spring profile for this run:
./mvnw -pl spring-boot-training spring-boot:run -Dspring-boot.run.profiles=local
```

**`spring-boot:build-image`** builds an OCI image with Cloud Native Buildpacks, no
Dockerfile, using the layered jar for cacheable layers:

```bash
./mvnw -pl spring-boot-training spring-boot:build-image \
  -Dspring-boot.build-image.imageName=codelearn/spring-boot-training
```

It needs a running Docker (or podman) daemon. `repackage` (the executable fat jar) is
already bound to the `package` phase by the parent, so `./mvnw package` produces
`target/spring-boot-training-*.jar` that runs with `java -jar`.

**Build and git metadata.** Two goals are wired in the module pom:

| Goal                     | Output                           | Actuator surface         |
|--------------------------|----------------------------------|--------------------------|
| `spring-boot:build-info` | `META-INF/build-info.properties` | `/actuator/info` `build` |
| `git-commit-id:revision` | `target/classes/git.properties`  | `/actuator/info` `git`   |

Both run on every build (including `test`), so after any `./mvnw` invocation
`GET /actuator/info` shows the artifact version, build time, branch, and commit.
`git.mode: full` in `application.yml` expands the git block. The `git-commit-id` plugin
inherits its version, `revision` execution, and configuration from the Boot parent; the
module pom only lists it to switch it on.

**Profile activation example.** The module pom defines two Maven profiles:

- `local` uses `<activeByDefault>true</activeByDefault>` — active unless another profile
  is explicitly selected. Selecting any other profile with `-P` silently switches it off;
  that is the classic `activeByDefault` footgun.
- `ci` uses `<activation><property><name>env.CI</name></property></activation>` — turns
  itself on whenever the `CI` environment variable is set, no `-P` needed.

Each profile only sets `spring-boot.run.profiles` (the Spring profile `spring-boot:run`
starts with). Force a profile explicitly with `-Plocal` / `-Pci`, list active ones with
`./mvnw -pl spring-boot-training help:active-profiles`.

**Banner version.** Boot's built-in `${application.version}` banner token reads the jar
manifest `Implementation-Version`, which only exists in the packaged jar, so it is blank
under `spring-boot:run`. Instead the banner uses `@project.version@`, expanded by Maven
resource filtering at build time — `banner.txt` is added to the filtered `<resources>` in
the reactor parent (alongside `application*.yml`), using the `@...@` delimiter so Spring's
own `${...}` tokens (`${AnsiColor.*}`, `${spring-boot.formatted-version}`) pass through
untouched. Result: the version shows for both `spring-boot:run` and the built jar. Build
and git details still belong in `/actuator/info`, not the banner.

### 1.3 Project setup

**Config file layout.**

| File                    | Loaded when                                          | Holds                                                                    |
|-------------------------|------------------------------------------------------|--------------------------------------------------------------------------|
| `application.yml`       | always                                               | shared defaults, prod-safe (structured logging on)                       |
| `application-local.yml` | `local` profile (`./mvnw spring-boot:run`, see 1.2)  | dev ergonomics: readable logs, `DEBUG`, all actuator endpoints, DevTools |
| `application-test.yml`  | tests that declare `@ActiveProfiles("test")`, and CI | quiet logging, banner off, test-only `app.*` values                      |

Profile files override, not replace: a `local` run still gets everything in
`application.yml` plus the `local` overrides. Secrets never go in any of these; they come
from the environment or a git-ignored `.env` (the root `.gitignore` has an `.env` block).

**Typed configuration.** `config/AppProperties` is a `record` annotated
`@ConfigurationProperties("app")` + `@Validated`. `@ConfigurationPropertiesScan` on the
application class registers it (no `@EnableConfigurationProperties` list). It shows:

- immutable binding into a record and a nested record (`Catalog`, `Cors`);
- `@DefaultValue` so `application.yml` only carries overrides (`app.catalog.*` can be
  omitted entirely and the nested record still binds);
- Jakarta constraints (`@NotBlank`, `@NotEmpty`, `@Positive`, `@Max`) enforced at startup —
  a bad value aborts the context with a readable report instead of failing later. Relaxed
  binding maps `app.catalog.default-page-size` (yaml) to `defaultPageSize` (record).

Prefer this over scattered `@Value`. `AppPropertiesTest` uses `ApplicationContextRunner` to
cover both binding and a validation failure without a full `@SpringBootTest`.

**Structured logging.** `application.yml` sets
`logging.structured.format.console: ecs` — native Boot structured logging (no Logback XML,
no Logstash encoder dependency), emitting Elastic Common Schema JSON to stdout with
`service.name/version/environment`. Once tracing is wired it also carries `traceId`/`spanId`
so Loki can correlate. `application-local.yml` and `application-test.yml` set
`logging.structured.format.console:` empty to fall back to the human-readable pattern.

**DevTools.** `spring-boot-devtools` is an `optional`, `runtime` dependency: automatic
restart on a classpath change and LiveReload while developing. Boot disables it in a fully
packaged jar and `optional=true` keeps it off any downstream classpath, so it is safe to
leave declared. `application-test.yml` turns restart off for tests.

**Docker Compose / Oracle** (a `compose.yaml` Oracle service) is deferred to U3.1, where the
first real datasource lands and there is something for it to wire.

---

## U2 — Core Spring Framework Concepts

### 2.1 Dependency injection

Lives in `foundation/di/` — a set of throwaway `Notifier` beans (`email`, `sms`, and a
`local`-only `console`) plus one `NotificationDispatcher` that consumes every wiring style.

**Constructor injection only.** `NotificationDispatcher` has a single constructor, so no
`@Autowired` is needed. No field or setter injection anywhere in the codebase (CLAUDE.md
rule); constructor injection keeps collaborators `final` and the object valid once built.

**`@Primary` vs `@Qualifier`.** Three beans implement `Notifier`:

| Injection point                      | Resolves to     | Why                                    |
|--------------------------------------|-----------------|----------------------------------------|
| `Notifier` (bare)                    | `EmailNotifier` | it is `@Primary`                       |
| `@Qualifier("smsNotifier") Notifier` | `SmsNotifier`   | an explicit qualifier beats `@Primary` |

The qualifier value is the default bean name (`smsNotifier`). A dedicated `@Qualifier`
annotation would work too; bean name is the least ceremony for a demo.

**`List<T>` and `Map<String, T>` injection.** `List<Notifier>` gets every implementation,
sorted by `@Order` on each bean (`email` 10, `sms` 20, `console` 30). `Map<String, Notifier>`
gets the same beans keyed by bean name. Both are copied defensively in the constructor.

**`ObjectProvider<T>` for an optional bean.** `ConsoleNotifier` is `@Profile("local")`, so it
is absent under `test` / default. `ObjectProvider<ConsoleNotifier>.getIfAvailable()` returns
`null` there instead of throwing `NoSuchBeanDefinitionException`, and resolves the bean lazily
when the `local` profile is on. Use it for 0-or-1 optional collaborators and to break eager
init.

**`@Profile`-scoped bean.** `ConsoleNotifier` only joins the context when `local` is active.
`DependencyInjectionLocalProfileTest` runs with `@ActiveProfiles({"test", "local"})` and sees
three notifiers; `DependencyInjectionTest` (`test` only) sees two.

**Circular dependencies.** `CircularDependencyExample` is a commented note: two components
that need each other via the constructor. Since Boot 2.6 that is rejected by default, so the
context fails fast with a "form a cycle" report. Fixes, best first: break the cycle (extract a
third bean, or use an event), inject one side `@Lazy`, or — last resort —
`spring.main.allow-circular-references=true` (only rescues setter/field injection).

Run the demo and watch the `NotificationDispatcher` `@PostConstruct` line:

```bash
./mvnw -pl spring-boot-training spring-boot:run
# DI demo wired: default=email, urgent=sms, channels=[email, sms, console], console-present=true
```

### 2.2 Beans

Lives in `foundation/beans/` — a `@Configuration` class (`BeansConfig`) wiring most of these via
`@Bean` methods, plus a few `@Component`-scanned beans for the styles a `@Bean` method can't show.

**Scope.** `BeansConfig` declares two `Widget` beans off the same class: `standardWidget()` (no
`@Scope`, so singleton — one instance, created eagerly at context refresh) and `customWidget()`
(`@Scope(SCOPE_PROTOTYPE)` — a new instance on every `getBean` call or injection point, created
lazily, no destroy callback). `BeanScopeTest` asserts `isSameAs` / `isNotSameAs` across two lookups
of each.

**Lifecycle hooks — three styles, same effect:**

| Style                      | Where                     | Init             | Destroy                    |
|----------------------------|---------------------------|------------------|----------------------------|
| JSR-250 annotations        | `AnnotationLifecycleBean` | `@PostConstruct` | `@PreDestroy`              |
| Callback interface         | `InterfaceLifecycleBean`  | —                | `DisposableBean.destroy()` |
| External wiring on `@Bean` | `ThirdPartyResource`      | `initMethod`     | `destroyMethod`            |

The `@Bean(initMethod = ..., destroyMethod = ...)` style is for classes you cannot annotate (a
library type); the other two are for your own classes. `BeanLifecycleTest` captures each bean
reference inside `ApplicationContextRunner.run(...)`, then asserts the destroy flag *after* the
runner closes the context, proving the callback actually fired.

**`@Lazy`.** `lazyGreeter()` is `@Lazy`, so `LazyGreeter`'s constructor (and its static construction
counter) only runs on first `getBean`, not during context refresh. `LazyBeanTest` checks the counter
before and after touching the bean.

**`@ConditionalOnProperty`.** `premiumSupportChannel()` only registers when
`app.foundation.premium-support=true`. `ConditionalBeanTest` exercises absent (default), present (`=true`), and explicitly absent (`=false`)
with `ApplicationContextRunner.withPropertyValues(...)`.

**`FactoryBean<T>`.** `WidgetFactoryBean implements FactoryBean<Widget>` for when *building* the
object needs more than a constructor call. `getBean("widgetFactoryBean")` (the default `@Component`
name) returns the **product** (a `Widget`); `getBean("&widgetFactoryBean")` returns the factory
itself — `WidgetFactoryBeanTest` checks both.

**`BeanPostProcessor`.** `LoggingBeanPostProcessor` runs `postProcessAfterInitialization` for every
bean in the context (filtered to this project's own packages) and records the bean names it saw.
`LoggingBeanPostProcessorTest` confirms the eager singletons (`standardWidget`,
`thirdPartyResource`) show up while the untouched `prototype` / `@Lazy` beans (`customWidget`,
`lazyGreeter`) do not — the post-processor only ever sees beans that actually got created.

### 2.3 Components

Lives in `foundation/components/` — the component stereotypes plus the container services that build on
them (events, scheduling, async, AOP). `ComponentsConfig` carries `@EnableAsync` and `@EnableScheduling`
for the whole application context; that is where those two features get switched on.

**Stereotypes are one scan.** `MessageProvider` has three implementations, one per stereotype:

| Bean                     | Annotation    | What the annotation adds beyond `@Component`                         |
|--------------------------|---------------|----------------------------------------------------------------------|
| `StaticMessageComponent` | `@Component`  | nothing; it is the generic marker                                    |
| `GreetingService`        | `@Service`    | semantic only; `ServiceTimingAspect`'s pointcut keys off it          |
| `MessageRepositoryStub`  | `@Repository` | persistence-exception translation (dormant until a datasource lands) |

`@Service` and `@Repository` are themselves meta-annotated `@Component`, so a single component scan finds
all three and an injected `List<MessageProvider>` (`StereotypeInventory`) holds them all.
`ComponentStereotypeTest` checks that with `AnnotatedElementUtils.hasAnnotation(...)`.

**Three ways to read config.** `ConfigAccessDemo` reads the same key, `app.name`, three ways:

| Style                                 | How                                | Trade-off                                                |
|---------------------------------------|------------------------------------|----------------------------------------------------------|
| `@Value("${app.name}")`               | resolved once at construction      | terse, but scatters string-literal keys through the code |
| `Environment#getProperty("app.name")` | programmatic lookup, may be `null` | good when the key name is computed                       |
| `AppProperties#name()`                | typed, validated record            | the house default: keys bound and validated in one place |

All three return the same string; `ConfigAccessDemoTest` asserts that against the `application-test.yml`
value.

**Application events.** `GreetingEventPublisher` wraps `ApplicationEventPublisher`; `GreetingEvent` is a
plain `record` (no need to extend `ApplicationEvent` since Spring 4.2). `GreetingEventListener` receives
it three ways:

| Listener                | Annotation                                  | Runs                                                  |
|-------------------------|---------------------------------------------|-------------------------------------------------------|
| `onGreeting`            | `@EventListener`                            | synchronously, on the publishing thread               |
| `onGreetingAsync`       | `@Async @EventListener`                     | on the task executor, off the publisher's stack       |
| `onGreetingAfterCommit` | `@TransactionalEventListener(AFTER_COMMIT)` | after the surrounding transaction commits (see below) |

`ApplicationEventsTest` asserts the sync listener runs on the caller thread and the async one does not.
The transactional listener is wired but stays dormant: there is no transaction manager in this module
yet, so it is skipped at publish time. U3.4 exercises it for real once JPA brings a transaction manager.

**`@Scheduled`.** `HeartbeatTask.beat()` runs on a timer. The interval is externalised as
`app.foundation.heartbeat-interval` (`fixedDelayString`), defaulting to `1h` so a running app logs one
beat at startup; `application-test.yml` shortens it to `200ms` so `SchedulingAndAsyncTest` does not wait.
Needs `@EnableScheduling`.

**`@Async`.** `AsyncGreeter.greetAsync(...)` returns a `CompletableFuture`; Spring's proxy hands the call
to the executor and returns immediately. Same proxy rules as `@Transactional`: no effect on
self-invocation, bean methods only. The executor is Boot's virtual-thread `SimpleAsyncTaskExecutor`
(`spring.threads.virtual.enabled=true`). Needs `@EnableAsync`.

**AOP timing aspect.** `ServiceTimingAspect` is an `@Around` aspect that logs elapsed millis for
`@Service` methods. `spring-boot-starter-aspectj` (Boot 4's rename of `spring-boot-starter-aop`) puts
`aspectjweaver` on the classpath, which is what activates Boot's AOP auto-configuration. The pointcut is
scoped to `@Service` beans *in this package only*, so it will not silently start timing the real
services added in U3/U4. `ServiceTimingAspectTest` confirms a `@Service` call is timed and a
`@Repository` call is not.

Run it and watch the startup log:

```bash
./mvnw -pl spring-boot-training spring-boot:run
# MessageProvider beans found by component scan: {StaticMessageComponent=component, GreetingService=service, MessageRepositoryStub=repository}
# app.name via @Value='...', via Environment='...', via @ConfigurationProperties='...'
# Heartbeat #1 on '...'
```

---

## U3 — Data Persistence Layer

### 3.1 Datasource & migrations

The persistence stack lands here: an Oracle datasource on HikariCP, Flyway SQL-first
migrations, Hibernate in `validate` mode, and a Testcontainers base class for the tests.
No entities yet — those are 3.2. `V1__create_catalog.sql` creates the `catalog` slice (`category`, `product`); the orders schema follows as
`V2` with its entities.

**Where the datasource comes from.** The app assumes Oracle is already running — it does
not start one.

| Context                  | Datasource                                                                                                    |
|--------------------------|---------------------------------------------------------------------------------------------------------------|
| `./mvnw spring-boot:run` | `spring.datasource.*` in `application.yml` → `localhost:1521/FREEPDB1` (start it with `docker compose up -d`) |
| Integration tests        | `AbstractOracleIntegrationTest` — a Testcontainers `@ServiceConnection` container                             |
| Production               | environment (`SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD`) overrides the committed defaults            |

The committed `url` / `username` / `password` are the `compose.yaml` values (the `training`
app user in `FREEPDB1`) — a throwaway local credential, not a secret. Everything else in `application.yml` is HikariCP pool tuning
(`pool-name`, `maximum-pool-size`,
`minimum-idle`, `connection-timeout`, `max-lifetime`),
sized deliberately small: this is a demo, and virtual threads make a connection wait cheap.
Size a real pool to the database's `max_connections` and the app's true concurrency, not to
request volume.

**Local database (`compose.yaml`).** A convenience, nothing more — bring it up with
`docker compose up -d`, take it down with `docker compose down`. The app does not manage it (no `spring-boot-docker-compose`), so it must be
running before `spring-boot:run` or
`flyway:migrate`. The image is `gvenzl/oracle-free:23-slim` — Oracle Database 23ai Free, slim
variant (no APEX / sample schemas, smallest image and fastest first boot). `APP_USER` /
`APP_USER_PASSWORD` auto-create the `training` user in the default `FREEPDB1` pluggable
database. Oracle-container best practice baked into the service: `shm_size: 1g` (Oracle needs
far more than Docker's 64 MB `/dev/shm`), the image's own `healthcheck.sh`, a named
`oracle-data` volume at `/opt/oracle/oradata` so the database survives `down` (`down -v` to
wipe), and a first-boot init hook at `./compose/oracle-init` → `/container-entrypoint-initdb.d`
(empty for now). First-ever boot creates the database and takes a few minutes; the healthcheck
`start_period` allows for it.

**Flyway — manual, not on boot.** SQL migrations live in `src/main/resources/db/migration`,
named `V<n>__<snake_case>.sql`. `spring.flyway.enabled: false` in `application.yml`, so the
app never migrates when it starts. Instead the `flyway-maven-plugin` (module `pom.xml`) runs
them against a running database on demand:

```bash
docker compose up -d                                  # or let a previous `spring-boot:run` leave it up
./mvnw -pl spring-boot-training flyway:migrate         # apply pending migrations
./mvnw -pl spring-boot-training flyway:info            # applied / pending state
./mvnw -pl spring-boot-training flyway:validate        # scripts vs. history
./mvnw -pl spring-boot-training flyway:clean           # drop everything (dev only)
```

The plugin has no lifecycle-bound execution, so a normal build never triggers it. Its
connection defaults (`jdbc:oracle:thin:@//localhost:1521/FREEPDB1`, `training`/`training`)
match `compose.yaml`; override with `-Dflyway.url` / `-Dflyway.user` / `-Dflyway.password`
or a `flyway.conf`. Plugin version tracks `flyway-core` via the Boot BOM; the Oracle JDBC
driver (`ojdbc17`) and `flyway-database-oracle` (Flyway 10+ ships no DB dialects in core) are
plugin dependencies, versioned from the BOM properties `oracle-database.version` /
`flyway.version`.

**Sample data — baseline vs demo.** Seeding is two-tier, and both tiers are plain SQL that
Flyway owns (no `ddl-auto`, no `data.sql`, no seeder bean):

| Tier          | Location                 | Loaded by                                                               | Size                                            |
|---------------|--------------------------|-------------------------------------------------------------------------|-------------------------------------------------|
| Baseline seed | `db/migration/R__0*.sql` | everyone — `flyway:migrate` **and** the `test` profile (Testcontainers) | tiny: 6 categories, 24 products (orders in 3.2) |
| Demo bulk     | `db/demo/R__9*.sql`      | local Oracle via `flyway:migrate`; **never** the `test` profile         | ~500 generated products                         |

The `flyway-maven-plugin` `<locations>` in the module `pom.xml` lists **both** directories, so
`./mvnw flyway:migrate` against the local Oracle applies both tiers, so running the app from the
IDE gives you the full dataset. The Testcontainers integration tests get the baseline only,
because Boot's Flyway (the only one active in the `test` profile) reads
`spring.flyway.locations: classpath:db/migration` and never scans `db/demo`.

The seeds are **repeatable** migrations (`R__`), not versioned: Flyway re-applies one whenever
its checksum changes, so you edit the seed in the same commit as the `V*` DDL that motivated it
and never juggle version numbers. Each script is idempotent: the baseline uses one `MERGE` per
table keyed on the natural key (`category.name`, `product.sku`); the demo bulk owns every
`GEN-%` SKU, deletes them, and regenerates them with a seeded `DBMS_RANDOM` so prices reproduce.
Repeatable migrations run after all versioned ones, in description order (`R__010_…` before
`R__910_…`).

```bash
./mvnw -pl spring-boot-training flyway:migrate                  # DDL + baseline + demo bulk
./mvnw -pl spring-boot-training flyway:info                     # applied / pending state
./mvnw -pl spring-boot-training -Dflyway.cleanDisabled=false flyway:clean   # then flyway:migrate to reset
```

`flyway:clean` is disabled by default (Flyway 10+); pass `-Dflyway.cleanDisabled=false` to drop
and rebuild. Integration tests that assert on exact contents still add their own fixture with
`@Sql` (`src/test/resources`, per 3.6) and check their own marked rows, not global counts.
`CatalogSeedIT` verifies the baseline is present and that no `GEN-%` rows leak into the test
database.

**Adding an entity later:** ship its `V*` DDL and extend the matching `R__0*` baseline seed in
the same pass; add `db/demo` bulk rows when the feature needs volume for a demo.

**Tests are the exception.** `application-test.yml` sets `spring.flyway.enabled: true`. The
test datasource is a per-run Testcontainers database on a random port, so `mvn
flyway:migrate` cannot reach it — Boot's `FlywayAutoConfiguration` migrates it on context
start instead. That autoconfig comes from `org.springframework.boot:spring-boot-flyway`
(Boot 4 splits each integration's autoconfig into its own module; it pulls `flyway-core`).

`V1` adds `created_at` / `updated_at` / `version` columns up front so the 3.2 entities (JPA
auditing, `@Version`) map onto them without an `ALTER`.

**`ddl-auto: validate`.** Migrations own the schema; Hibernate only checks the entity
mappings against it and fails fast on a mismatch. Never `create` / `update` /
`create-drop`. With no entities yet, `validate` is a no-op. Because migrations no longer run
on boot, starting the app against an un-migrated database will fail `validate` once 3.2 adds
entities — that is the intended "did you run `flyway:migrate`?" signal.
`DatasourceMigrationIT` (test profile, so Flyway ran) asserts `flyway_schema_history`, the
tables, and the FK all exist.

**`open-in-view: false`.** The persistence context closes at the end of the service call,
not the view render, so a lazy access from a controller or serializer fails loudly instead
of firing surprise queries.

**Testcontainers base class.** `AbstractOracleIntegrationTest` (`support/`) starts one
`gvenzl/oracle-free:23-slim` container per JVM (singleton pattern: a `static` field, started
in a static initialiser, reaped by Ryuk at exit) and exposes it via `@ServiceConnection`. Every
`@SpringBootTest` in the module now extends it — a full context needs a real database to
start once a datasource is on the classpath. The `ApplicationContextRunner` tests (`AppPropertiesTest`, the `foundation.beans` tests)
don't — they never build a full context.
`*IT` classes run under Failsafe (`maven-failsafe-plugin`, switched on in the module pom);
`*Test` classes stay on Surefire.

**Transaction manager.** `spring-boot-starter-data-jpa` finally brings a real
`JpaTransactionManager`, so the `@TransactionalEventListener` wired dormant in 2.3 is now
active (still only fires inside an actual transaction — exercised in 3.4).

```bash
docker compose up -d                                   # Oracle must be up first
./mvnw -pl spring-boot-training flyway:migrate          # apply migrations (once, or after adding a script)
./mvnw -pl spring-boot-training spring-boot:run
# (no Docker Compose or Flyway output at boot)
# GET /actuator/health -> {"components":{"db":{"status":"UP"}, ...}}
```

### 3.2 Spring Data JPA — model & associations

The domain object model for the Catalog & Orders slice — five entities in one flat
`catalog` package, plus `V2__create_orders.sql` and the paired orders seed. Repositories and
queries are 3.3; services and transactions are 3.4. No controllers yet.

```
Category 1 ──< Product           Product.category  @ManyToOne  ·  Category.products  @OneToMany (inverse)
Customer 1 ──< Order             Order.customer    @ManyToOne
Order    1 ──< OrderItem >──1 Product              @OneToMany(cascade = ALL, orphanRemoval = true)  +  @ManyToOne
                                 → Order ↔ Product is many-to-many, resolved through the OrderItem join entity
Address  @Embeddable             Customer.billingAddress  ·  Order.shippingAddress (@AttributeOverrides → ship_*)
OrderStatus  enum                Order.status  @Enumerated(STRING)  + ck_order_status check constraint
```

**One `@MappedSuperclass` for the boilerplate.** `AbstractAuditableEntity` gives every entity
the four columns that `V1`/`V2` put on every table:

| Field                     | Mapping                                              | Notes                                                                                       |
|---------------------------|------------------------------------------------------|---------------------------------------------------------------------------------------------|
| `id`                      | `@Id @GeneratedValue(IDENTITY)`                      | Oracle `generated always as identity`. Identity keys disable Hibernate insert batching.     |
| `version`                 | `@Version` `Long`                                    | 0 on insert, +1 on every dirtying flush; stale value on update → `OptimisticLockException`. |
| `createdAt` / `updatedAt` | `@CreatedDate` / `@LastModifiedDate`, type `Instant` | Spring Data JPA auditing (`@EnableJpaAuditing` in `config/persistence/PersistenceConfig`).  |

Auditing needs `@EnableJpaAuditing` present or the listener never runs. The timestamp type is **`Instant`, not `OffsetDateTime`** — Spring
Data's auditing converter supports
`LocalDateTime` / `Instant` / `Date` / `Long` only. `@JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE)`
keeps the column mapped to the `timestamp with time zone` type Flyway created in `V1`.

**Associations.** All `@ManyToOne` are `fetch = LAZY` (overriding the JPA default of EAGER) and
`optional = false`, so the FK column is `NOT NULL`. `Order.items` is the only rich `@OneToMany`:
`cascade = ALL` + `orphanRemoval = true`, mutated through `order.addItem(product, qty, price)` /
`order.removeItem(item)` helpers that also refresh the cached `totalAmount`. `Category.products`
is the thin inverse side — `mappedBy`, no cascade to delete, read-mostly.

**`OrderItem` is a join entity, not a `@ManyToMany`.** `Order` ↔ `Product` is conceptually
many-to-many, but the link row carries `quantity` and `unitPrice` (captured at order time), so
it is modelled as an entity with two `@ManyToOne`s and a `uq_order_item_order_product` unique
constraint. This is the standard "many-to-many with attributes" pattern.

**`Address` is a `record`, entities are Lombok.** `Address` is an immutable `@Embeddable`
value object → a Java record (Hibernate 7 maps record embeddables for `@Embedded`). The five
entities are the one place Lombok is allowed: `@Getter` / `@Setter` on the mutable fields,
`@NoArgsConstructor(access = PROTECTED)`, **no** `@Data` / `@EqualsAndHashCode` / `@ToString`.
`equals` / `hashCode` are hand-written against the **business key** (`Category.name`,
`Product.sku`, `Customer.email`, `Order.orderNumber`, `OrderItem` = order + product), which has
no setter so it stays stable in a `HashSet`.

**`order` is a reserved word.** `Order` wraps its `@Table` name in backticks — Hibernate's
portable "quote this identifier" marker — and emits `"order"` on Oracle. `V2`, both seeds, and
`OrdersSeedIT` all quote it the same way. Oracle folds unquoted identifiers to upper case, so
from SQL the table is always `"order"` (quoted, lower-case).

**Orders seed — same two-tier convention as the catalog (3.1).** `V2` ships with its paired
baseline seed in the same commit:

| Tier          | File                                  | Loaded by                             | Contents                                                                                              |
|---------------|---------------------------------------|---------------------------------------|-------------------------------------------------------------------------------------------------------|
| Baseline seed | `db/migration/R__020_seed_orders.sql` | everyone (incl. Testcontainers tests) | 3 customers, 3 `SEED-*` orders, 5 lines; `MERGE` on the natural keys, line prices read from `product` |
| Demo bulk     | `db/demo/R__920_demo_orders.sql`      | local `flyway:migrate` only           | ~200 `GEN-*` orders, 1–3 lines each, seeded `DBMS_RANDOM`                                             |

`R__020` recomputes each seeded order's `total_amount` from its lines at the end, so the cached
column always matches. `OrdersSeedIT` asserts the baseline is present, consistent, and that no
`GEN-*` rows leak into the test database.

**Verification.** `CatalogDomainMappingIT` (a `@Transactional` `*IT`) persists a `Customer` +
`Order` + two `OrderItem`s off seeded products, round-trips through `flush` / `clear`, and
asserts the reloaded graph: item count, `totalAmount`, lazy nav, auditing fields, `@Version`
starting at 0 then incrementing, and `orphanRemoval` deleting a line row. Every other
`@SpringBootTest` in the module also boots the full context under `validate`, so a mapping that
disagrees with `V1`/`V2` fails the whole build.

### 3.3 Spring Data JPA — repositories & queries

The query layer for the Catalog & Orders slice: four repositories (one per aggregate root —
`Category`, `Product`, `Customer`, `Order`; `OrderItem` is reached only through `Order`), plus
the projection and `Specification` types they return. All package-private in `catalog`, since
the entities are. No services yet (3.4), no `@DataJpaTest` yet (3.6) — the coverage here is
full-context `*IT` against real Oracle, matching 3.2.

**One example of every query style.** `ProductRepository` is the exhibit:

| Style                         | Method                                                              | Note                                                                      |
|-------------------------------|---------------------------------------------------------------------|---------------------------------------------------------------------------|
| Derived (name → `where`)      | `findBySku`, `existsBySku`, `findByPriceBetween`, `countByCategory` | parsed at startup; a bad name fails the context, not a call               |
| Derived across an association | `findByCategoryNameIgnoreCase`                                      | walks `product.category.name` by property path                            |
| Derived into an `@Embedded`   | `CustomerRepository.findByBillingAddressCountry`                    | resolves to the `country` column on `customer`                            |
| `Sort` parameter              | `findByNameContainingIgnoreCase(fragment, Sort)`                    | ordering pushed to the DB, not sorted in memory                           |
| `Pageable`                    | `findByCategory(Category, Pageable)`                                | runs the slice **and** a `count(*)`; use `Slice` to skip the count        |
| `@Query` JPQL                 | `findAtOrAbove`                                                     | explicit HQL when a derived name would be unwieldy                        |
| `@Query` native               | `findMostExpensiveInCategory`                                       | Oracle SQL for a correlated subquery; trades portability + mapping checks |
| `@EntityGraph`                | `findAllWithCategory`                                               | fetch graph to collapse an N+1 (below)                                    |
| `JpaSpecificationExecutor`    | `findAll(Specification, …)`                                         | dynamic `where`, see below                                                |

**Three projection styles.** A projection selects only the columns it names — no entity is
built:

- **Interface (closed)** — `ProductSummary` (`getSku`/`getName`/`getPrice`). Every getter maps
  to a property path; an *open* projection (`@Value("#{...}")`) would load the whole entity first.
- **Record (DTO)** — `ProductPriceView`, filled by a JPQL constructor expression
  (`select new gr.codelearn.…ProductPriceView(p.sku, p.name, c.name, p.price) …`). The
  fully-qualified name is required — JPQL has no imports. A plain value the caller owns.
- **Dynamic** — `findBySkuStartingWith(prefix, Class<T>)`: the same query returns `Product` or
  `ProductSummary` depending on the `Class` argument.

`OrderRepository.summariseByStatus()` shows an **aggregate** query bound to an interface
projection: `select o.status as status, count(o) as orderCount, … group by o.status`, each
column aliased to an `OrderStatusSummary` getter name.

**Dynamic filtering with `Specification`.** `ProductSpecifications` holds one-predicate
fragments (`nameContains`, `priceAtLeast`, `priceAtMost`, `inCategory`); an absent argument
returns `Specification.unrestricted()` (a no-op) instead of `null`, so the caller composes the
non-blank ones with `Specification.allOf(…)` without null checks. This is the Criteria-API
answer to "a search form where any field may be empty" — the dynamic-query logic stays out of
both the repository and the caller.

**`@EntityGraph` and the N+1, measured.** `product.category` is `LAZY`, so looping `findAll()`
and touching `getCategory().getName()` fires one select for the products plus one per distinct
category. `findAllWithCategory()` attaches `@EntityGraph(attributePaths = "category")`, so
Hibernate loads every category in the same join. `ProductRepositoryIT.entityGraphCollapsesTheNPlusOne`
turns on Hibernate's statement counter and asserts the graph path runs **exactly one** statement
while the lazy path runs strictly more — the teaching point is verified, not just claimed.

**Verification.** `ProductRepositoryIT` (7 tests) and `OrderRepositoryIT` (4) run `@Transactional`
against the `R__010`/`R__020` baseline (rolled back, so the shared seed is untouched); the
`LazyInitializationException` contrast test proves what the graph buys you. `./mvnw -pl
spring-boot-training verify` → surefire 36, failsafe 25 (+11), all green.

### 3.4 Transactions

The first **service layer** in the catalog slice. `OrderService` is the exhibit — one method
per transaction concept — with `OrderFactory` (builds the `Order` aggregate from a
`PlaceOrderCommand`) and `OrderPlacementService` (`REQUIRES_NEW`) as collaborators. All
package-private in `catalog`, but every `@Transactional` method is **`public`**: Spring's proxy
only advises public methods, so `@Transactional` on a package-private method compiles and runs
with no transaction at all.

`@Transactional` lives on the service, never on the controller or the repository. Input is a
`PlaceOrderCommand` record (a command object); output is an `OrderConfirmation` record, never
the `Order` entity.

| Method                         | Setting                                        | Point                                                                                                                                                                          |
|--------------------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `placeOrder`                   | `@Transactional`                               | baseline read-write tx; saves the graph, publishes `OrderPlacedEvent`                                                                                                          |
| `findOrder`                    | `readOnly = true`                              | Hibernate flush mode → `MANUAL`, connection flagged read-only; use it for every query method                                                                                   |
| `peekStatusAfterLocalEdit`     | `readOnly = true`                              | mutates a loaded entity and returns the new value, but the read-only tx **never flushes** — reload and the row is unchanged. A dirty write here is silently lost, not rejected |
| `placeOrderAndDeliverReceipt`  | `rollbackFor = ReceiptDeliveryException.class` | the default rule **commits** on a checked exception; `rollbackFor` opts in to rollback                                                                                         |
| `placeOrderToleratingLowStock` | `noRollbackFor = LowStockException.class`      | the default rule **rolls back** on a `RuntimeException`; `noRollbackFor` opts out, so the order commits and the exception still propagates                                     |
| `placeBatchIndependently`      | *(none)* — delegates to `REQUIRES_NEW`         | each order commits in its own transaction (via `OrderPlacementService`), so one bad order does not undo the ones already placed                                                |
| `placeBatchAtomically`         | `@Transactional`                               | the contrast: one transaction for the batch, first bad order rolls back the lot                                                                                                |
| `probeTransactionActive`       | `@Transactional`                               | returns `TransactionSynchronizationManager.isActualTransactionActive()`                                                                                                        |

**`REQUIRES_NEW` needs a proxy hop.** `OrderPlacementService.placeIndependently` is a separate
bean, not a method on `OrderService`, on purpose. Propagation only takes effect when the call
crosses the Spring proxy; a `REQUIRES_NEW` method called on `this` shares the caller's
transaction as if the annotation were not there.

**Self-invocation pitfall.** `checkViaSelfInvocation()` calls `probeTransactionActive()` on
`this` → the call never reaches the proxy → no transaction → returns `false`.
`checkViaInjectedProxy()` calls it through an injected `@Lazy OrderService self` → the advice
runs → `true`. The fixes are all "go through a proxy": an injected collaborator (what
`placeBatchIndependently` does), an injected `self`, or a programmatic `TransactionTemplate`.

**No network or AI in a transaction.** An external call from a `@Transactional` method holds
the DB connection and the new rows' locks for the whole round trip, and its failure rolls the
business data back. `placeOrder` publishes `OrderPlacedEvent` and `OrderNotifications` handles
it with `@TransactionalEventListener(AFTER_COMMIT)` — the side effect (confirmation email,
payment, AI summary; a logged stub here) runs only after the order is durably committed, and
not at all if the transaction rolled back. This is the live `AFTER_COMMIT` demo the
`foundation.components` unit (2.3) left dormant.

**Verification.** `OrderTransactionSemanticsIT` (8 tests) runs against real Oracle and is **not**
`@Transactional` (a test-managed tx would mask commit / `REQUIRES_NEW` / rollback semantics);
it uses `TX-`-prefixed order numbers and deletes them in `@AfterEach`. `./mvnw -pl
spring-boot-training verify` → surefire 36, failsafe 33 (+8), all green.

### 3.5 Spring Data JDBC / JdbcClient read-model

A **reporting slice** that reads and writes without Hibernate - the other half of the Spring
persistence story, next to the JPA-heavy `catalog`. New flat package `reporting`, a `V3` schema (`sales_ledger` fact table + a
`report_snapshot` / `report_snapshot_line` aggregate) and its
`R__030` baseline seed. No controllers (U4), no `@DataJdbcTest` slice yet (that pattern is 3.6).

`spring-boot-starter-data-jdbc` sits on the classpath alongside `spring-boot-starter-data-jpa`.
Spring Data's **strict multi-store detection** is what routes each repository by its aggregate -
`@org.springframework.data.relational...Table` + `ListCrudRepository` goes to JDBC,
`@jakarta.persistence.Entity` + `JpaRepository` to JPA. The reporting aggregate uses the
`org.springframework.data.*` annotations only; a stray `jakarta.persistence` import would hand it
to the wrong module.

Detection alone works, but each scanner still *looks at* every repository interface and logs a
`Could not safely identify store assignment` INFO line for the ones it can't claim (4 lines for
the catalog repos). So `config/persistence` pins each module to its own package -
`@EnableJpaRepositories(basePackages = { "…catalog", "…order" })` on `PersistenceConfig`,
`@EnableJdbcRepositories(basePackages = "…reporting")` on `JdbcConfig` - which keeps startup
quiet and makes the split explicit. (`…catalog` was one package until the U4.1 follow-up split it
into sibling `catalog` / `order` modules - see below.) The Spring Data JDBC infrastructure beans still come from
Boot; the annotation only registers the repository config extension.

**JDBC vs JPA - what you trade.**

|                            | Spring Data JPA (`catalog`)                                                       | JdbcClient / Spring Data JDBC (`reporting`)                                                  |
|----------------------------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| Identity & change tracking | persistence context, dirty checking, automatic flush                              | none - you pass exactly the rows to write                                                    |
| Lazy loading               | `@ManyToOne(LAZY)`, proxies, `LazyInitializationException`                        | no lazy: an aggregate loads whole, a query returns exactly its columns                       |
| Aggregate boundary         | soft - you can navigate anywhere from any entity                                  | hard - the root owns its children, cascades delete + re-insert, nothing outside is reachable |
| SQL                        | generated (JPQL / criteria / derived names); tune with `@EntityGraph`, fetch mode | you write it; no surprises, no portability layer                                             |
| Mapping safety net         | `ddl-auto=validate` checks entities against the schema at boot                    | none - a column rename is a runtime `ORA-00904`                                              |
| First-level cache          | yes (repeated `findById` in one tx is free)                                       | no - every call hits the database                                                            |
| Reach for it when          | a domain you mutate through a rich object graph                                   | a report, a projection, a bulk load, a join no entity models                                 |

**The JdbcClient read.** `CategorySalesQuery` aggregates `sales_ledger ⋈ product ⋈ category`
into `CategorySales` records. `JdbcClient` is the fluent wrapper Boot auto-configures over
`JdbcTemplate`: `.sql(text).param("since", date).query(CategorySales.class).list()`. The
`select` aliases (`category_name`, `units_sold`, …) map onto the record components by relaxed
name, so an alias with no matching component is an error. `.query(BigDecimal.class).single()`
is the scalar form; `.optional()` tolerates zero rows but still rejects two.

**The Spring Data JDBC aggregate.** `ReportSnapshot` (record, `@Id`, `@MappedCollection Set<…>
lines`) freezes one query result. `SalesReportRepository extends ListCrudRepository`; `save`
writes the root then **replaces every child row**, `findByLabel` returns the root with all
lines populated (1 query for the root + 1 for the collection). `SnapshotService.capture` is
idempotent on the label - a repeat call loads the existing snapshot and re-saves it, which is
where the delete-and-re-insert of children shows.

**The batch insert.** `SalesIngestService.ingest(List<SalesLedgerEntry>)` bulk-loads the fact
table. `JdbcClient` has **no batch API** in Spring Framework 7.x, so this drops to
`NamedParameterJdbcTemplate.batchUpdate(sql, SqlParameterSource[])` - one prepared statement,
one round trip. The array is built with `SimplePropertySqlParameterSource` (record-aware);
`SqlParameterSourceUtils.createBatch` is **not** - it reads JavaBean getters and binds nothing
from a record. No generated keys are read back (the caller supplies `ledger_ref`), which also
sidesteps Oracle's identity-column-in-batch limitation.

**Oracle identifier casing.** Spring Data JDBC renders a `@Table("…")` / `idColumn` string
literal as a *quoted* identifier verbatim. Oracle folds unquoted DDL names to upper case, so
those literals are written `@Table("REPORT_SNAPSHOT")` - a quoted `"report_snapshot"` would not
resolve. Derived names (record component → column) are unquoted and fold on their own.
`JdbcClient` SQL is passed through literally, so its lowercase `sales_ledger` is fine.

```bash
docker compose up -d
./mvnw -pl spring-boot-training flyway:migrate          # V3 + R__030
./mvnw -pl spring-boot-training spring-boot:run         # no multi-store warnings at startup
```

**Verification.** `CategorySalesQueryIT` (5), `SalesIngestServiceIT` (3, **not** `@Transactional`

- the batch must commit; `IMP-`-prefixed rows, `@AfterEach` cleanup), `SnapshotServiceIT` (3,
  whole-aggregate round-trip + recapture + cascade delete), `persistence/ReportingSeedIT` (4,
  baseline present and each snapshot line == its category's ledger sum). `./mvnw -pl
spring-boot-training verify` → surefire 36, failsafe 48 (+15), all green.

### 3.6 Persistence testing

The three ways to test persistence code, cheapest first: a **JPA slice** (`@DataJpaTest`), a **JDBC slice** (`@JdbcTest`), and a full **
`@SpringBootTest` `*IT`** for the things a slice can't
reach.

**Boot 4.1 packaging.** Every test slice is its own module now, and none rides in on
`spring-boot-starter-test` (same as `spring-boot-webmvc-test`): add `spring-boot-data-jpa-test`
for `@DataJpaTest` (it transitively pulls `spring-boot-jpa-test` = `TestEntityManager`, at
`org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`) and `spring-boot-jdbc-test`
for `@JdbcTest` + `@AutoConfigureTestDatabase`. The annotations themselves moved too -
`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
`org.springframework.boot.jdbc.test.autoconfigure.JdbcTest`.

**Slices against real Oracle.** A slice replaces the datasource with an embedded database by
default; there is none on the classpath and the point is to test against Oracle, so every slice
gets `@AutoConfigureTestDatabase(replace = NONE)` and reuses the shared Testcontainers
`OracleContainer` (`support/SharedOracleContainer` - one container for the whole JVM, referenced
by both the `@SpringBootTest` base and the slice base). `@ActiveProfiles("test")` keeps
Boot-managed Flyway on: both slices import `FlywayAutoConfiguration` (via `@AutoConfigureJdbc` →
`@AutoConfigureDataSourceInitialization`), so the slice context migrates the schema + `R__0*`
baseline seed exactly like the full-context path.

**`@DataJpaTest` (`ProductRepositoryDataJpaTest`).** Loads Hibernate, the `catalog` repositories,
`TestEntityManager`, an `@Transactional` wrapper that rolls every method back - and nothing else
(`context.getBeanNamesForType(OrderService.class)` is empty). A slice does not load user
`@Configuration`, so `PersistenceConfig`'s `@EnableJpaAuditing` is gone and the `NOT NULL`
`created_at` / `updated_at` columns would fail on flush; a nested `@TestConfiguration
@EnableJpaAuditing` puts just that back (importing the package-private `PersistenceConfig` would
mean making it public for a test).

**`@JdbcTest` (`CategorySalesQueryJdbcTest`).** `DataSource` + `JdbcClient` +
`DataSourceTransactionManager` + rollback, no JPA (`EntityManagerFactory` bean absent). The
read-model query object is a `@Component`, not a repository, so it is not slice-scanned:
`@Import(CategorySalesQuery.class)` brings it in and it aggregates the `R__030` ledger seed.

**`@Sql` seed scripts.** `src/test/resources/sql/*.sql` (**not** `db/migration` - Flyway would
pick those up). Inside a transactional slice `@Sql` runs in the test's transaction and rolls back
with it (`extra-laptop.sql` adds a 5th Laptop the repo then sees; `extra-storage-ledger.sql`
feeds two March rows into the aggregate). On a non-transactional `*IT` method `@Sql` commits, so
a paired `@Sql(executionPhase = AFTER_TEST_METHOD)` script is the declarative form of an
`@AfterEach` cleanup (`PersistenceRollbackIT`).

**Explicit rollback verification (`PersistenceRollbackIT`).** Rather than trusting that an
`@Transactional` test rolls back, it drives `TestTransaction` by hand: insert a row, see it
inside the transaction, `TestTransaction.flagForRollback()` + `.end()`, then prove in a fresh
transaction that the row is gone.

**Verification.** `ProductRepositoryDataJpaTest` (4, surefire), `CategorySalesQueryJdbcTest` (3,
surefire), `persistence/PersistenceRollbackIT` (2, failsafe). `./mvnw -pl spring-boot-training
verify` → surefire 43 (+7), failsafe 50 (+2), all green.

## U4 — REST API Development

### 4.1 Controllers & REST endpoints

The first HTTP layer on the `catalog` slice: `CatalogController` and `OrderController` under
`/api/v1`, plus `CatalogService` (the read/write use cases for products and categories) and a new
`config.web` package (CORS, config-driven paging, OpenAPI metadata). DTOs and mapping are kept **thin** — plain `*Request` / `*Response`
records and hand-written `@Component` mappers (`CatalogWebMapper`, `OrderWebMapper`); U4.3 reshapes the records (Jackson 3, `@JsonView`,
nested
order lines, PATCH bodies) and U4.4 moves the mapping to MapStruct (both mappers keep their U4.1 names and method signatures — only the
implementation changes, from a hand-written class to a generated one). Bean Validation and central error handling are U4.2.

**Endpoints.**

| Verb + path                                 | Binding                                                        | Success                                                                 | Failure                            |
|---------------------------------------------|----------------------------------------------------------------|-------------------------------------------------------------------------|------------------------------------|
| `GET /api/v1/products`                      | `ProductFilter` + `Pageable` query params (`@ParameterObject`) | `200` `PagedModel<ProductResponse>`                                     | —                                  |
| `GET /api/v1/products/{sku}`                | `@PathVariable`                                                | `200`                                                                   | `404`                              |
| `POST /api/v1/products`                     | `@RequestBody ProductRequest` (`OnCreate` group)               | `201` + `Location`                                                      | `400` invalid, `409` duplicate sku |
| `PUT /api/v1/products/{sku}`                | path + `ProductRequest` (`OnUpdate` group)                     | `200`                                                                   | `400` invalid, `404`               |
| `PATCH /api/v1/products/{sku}`              | path + `ProductPatchRequest` (all fields optional, U4.3)       | `200`                                                                   | `400` invalid, `404`               |
| `DELETE /api/v1/products/{sku}`             | `@PathVariable`                                                | `204`                                                                   | `404`                              |
| `GET /api/v1/categories`                    | —                                                              | `200` `List<CategoryResponse>`                                          | —                                  |
| `GET /api/v1/categories/{name}`             | `@PathVariable`                                                | `200`                                                                   | `404`                              |
| `POST /api/v1/categories`                   | `@RequestBody CategoryRequest` (`OnCreate` group)              | `201` + `Location`                                                      | `400` invalid, `409`               |
| `PUT /api/v1/categories/{name}`             | path + `CategoryRequest` (`OnUpdate` group)                    | `200`                                                                   | `400` invalid, `404`               |
| `POST /api/v1/orders`                       | `@RequestBody PlaceOrderRequest`                               | `201` + `Location`                                                      | `404` unknown customer/product     |
| `GET /api/v1/orders/{orderNumber}`          | `@PathVariable`                                                | `200` `OrderDetailResponse` (Detail view, nested lines + address, U4.3) | `404`                              |
| `GET /api/v1/orders`                        | `?customerEmail=&status=` (`@RequestParam`)                    | `200` `List<OrderDetailResponse>` (Summary view, U4.3)                  | —                                  |
| `PATCH /api/v1/orders/{orderNumber}/status` | path + `UpdateOrderStatusRequest`                              | `200`                                                                   | `404`                              |

Controllers only bind, delegate, and shape the `ResponseEntity` — no repository, no entity, no
`@Transactional`. `ResponseEntity.created(uri)` / `.noContent()` for the `201`/`204` cases; the
`Location` URI is built with `ServletUriComponentsBuilder`. A status transition is a `PATCH` on a
`/status` sub-resource, **not** a `POST …/cancel` verb in the URL.

**Pagination is config, not code.** `WebConfig` registers a
`PageableHandlerMethodArgumentResolverCustomizer` that clamps every `Pageable` argument to
`app.catalog.max-page-size` and falls back to `app.catalog.default-page-size` — the concrete use of
the U1.3 `AppProperties.Catalog` guard-rails. `CatalogApiIT` asserts a `?size=50` request comes
back with `page.size = 10` under the `test` profile.

**Serialize `PagedModel`, never `PageImpl`.** `listProducts` wraps the service's `Page` in
`new PagedModel<>(page)` (Spring Data's stable DTO: `{ "content": [...], "page": { size, number,
totalElements, totalPages } }`). Returning a raw `Page` / `PageImpl` from a controller has an
unstable JSON contract and Spring Data logs a warning.

**Content negotiation.** The controllers declare `produces = application/json`; Jackson 3 is the
only message converter on the classpath, so an `Accept: application/xml` request is answered
`406 Not Acceptable` by MVC, no controller code involved.

**Error mapping was a U4.1 stopgap**, `@ResponseStatus` on `NotFoundException` and
`CatalogDuplicateException`. U4.2 (below) removes both and routes every exception through a
central `@RestControllerAdvice` as RFC 9457 `ProblemDetail`.

**OpenAPI.** `springdoc-openapi-starter-webmvc-ui` (3.1.1 — the 3.x line targets Spring Boot 4 /
Spring Framework 7; not Boot-BOM-managed, pinned via `springdoc-openapi.version` in the reactor
parent). Spec at `/v3/api-docs` (OpenAPI 3.1 JSON), UI at `/swagger-ui.html`. `config.web.OpenApiConfig`
supplies the document title/version/tags; `@Tag` / `@Operation` / `@ApiResponse` annotate the
controllers.

```bash
docker compose up -d
./mvnw -pl spring-boot-training spring-boot:run
# http://localhost:8080/swagger-ui.html   http://localhost:8080/v3/api-docs
curl -s 'localhost:8080/api/v1/products?size=3'
curl -i -X POST localhost:8080/api/v1/products -H 'Content-Type: application/json' \
  -d '{"sku":"DEMO-1","name":"Demo","description":null,"price":9.99,"categoryName":"Storage"}'
curl -i localhost:8080/api/v1/products/NOPE            # 404
curl -i -H 'Accept: application/xml' localhost:8080/api/v1/products/LAP-0001   # 406
```

**Verification.** Slices (`@WebMvcTest`, `MockMvcTester`, mocked services): `CatalogControllerTest`
(6), `OrderControllerTest` (5). HTTP integration (`extends AbstractOracleIntegrationTest` +
`@AutoConfigureMockMvc`, real Oracle + Flyway seed): `CatalogApiIT` (4), `OrderApiIT` (2).
`spring-boot-webmvc-test` is a new test dependency — Boot 4.1 keeps `@WebMvcTest` /
`@AutoConfigureMockMvc` out of `spring-boot-starter-test`. `./mvnw -pl spring-boot-training verify`
→ surefire 54 (+11), failsafe 56 (+6), all green.

#### Follow-up (refactor) — sibling `catalog` / `order` / `common` features

By U4.1 the single `catalog` package held ~40 files across two bounded contexts. It was split
into two sibling feature packages plus a shared kernel, **package-by-feature, flat** (CLAUDE.md):
the controller, service, repositories, entities, specifications and projections all sit at each
feature's package root, with only `dto/`, `mapper/` and `internal/` as sub-packages.

- **`catalog`** — `CatalogService`, `CatalogController`, `Product`, `Category`, the repositories,
  `ProductSpecifications`, `ProductSummary`, `ProductPriceView` at the root; `dto/`, `mapper/`,
  `internal/` (just `CatalogDuplicateException`).
- **`order`** — `OrderService`, `OrderController`, the `Order` aggregate (`Order`, `OrderItem`),
  `Customer`, `Address`, `OrderStatus`, the repositories, `OrderStatusSummary` at the root; `dto/`,
  `mapper/`, `internal/` (`OrderFactory`, `OrderPlacementService`, `OrderNotifications`, the
  command/result/event records, the transaction-demo exceptions).
- **`common`** — the shared kernel: `AbstractAuditableEntity` (`@MappedSuperclass` for both) and
  `NotFoundException` (was `CatalogNotFoundException`).

**Why flat, not layered sub-packages.** Java package access is not hierarchical — a sub-package
cannot see a package-private sibling of its parent, so any layered split (`domain/`, `repository/`,
`web/`, …) forces almost every type to be `public` and turns `internal/` into a naming convention
with no enforcement. Flat keeps `CatalogController` / `OrderController`, `OrderNotifications` and
the projections genuinely package-private. Real boundary enforcement would need Spring Modulith (a `verify()` test) — deferred.

`order` keeps one documented, one-way dependency on `catalog`: `OrderItem` holds a
`@ManyToOne Product` and `OrderFactory` reads `ProductRepository` to price a line at order time.
`PersistenceConfig`'s `@EnableJpaRepositories` now lists both feature packages. Pure move — no
behaviour change; the `CatalogDomainMappingIT` graph test moved to `order` as `DomainGraphMappingIT`.

### 4.2 Exception handling & validation

U4.1 shipped two `@ResponseStatus` stopgaps. This step removes both and turns on real Jakarta Bean
Validation everywhere a request enters the API: request bodies, path variables, and query params.

**Central error handling.** New `config.web.ApiExceptionHandler extends ResponseEntityExceptionHandler`,
`@RestControllerAdvice`, the one place any exception becomes an RFC 9457 `ProblemDetail`:

| Thrown by                                                         | `@ExceptionHandler`                                   | Status |
|-------------------------------------------------------------------|-------------------------------------------------------|--------|
| `NotFoundException`                                               | direct                                                | `404`  |
| `CatalogDuplicateException`                                       | direct                                                | `409`  |
| `MethodArgumentNotValidException` (a `@Valid` body failure)       | overrides the `ResponseEntityExceptionHandler` method | `400`  |
| `HandlerMethodValidationException` (a `@Validated` param failure) | overrides the `ResponseEntityExceptionHandler` method | `400`  |
| anything else                                                     | catch-all, logged at ERROR                            | `500`  |

The two validation overrides attach an `errors` extension to the `ProblemDetail` — a JSON array of
`{"field": ..., "message": ...}` built from `BindingResult.getFieldErrors()` (body) or
`HandlerMethodValidationException.getParameterValidationResults()` (params). Both a top-level and a
cascaded nested-body violation report the same shape, e.g. `shippingAddress.line1` or
`lines[0].quantity`.

**Two different exceptions for two different validation triggers — a common trap.** A `@Valid`
`@RequestBody` failure throws `MethodArgumentNotValidException`, unchanged since Spring 6. A
`@Validated`/constrained `@PathVariable` or `@RequestParam` failure throws
`HandlerMethodValidationException` — **not** `jakarta.validation.ConstraintViolationException`,
which is what `jakarta.validation.Validator` throws directly and what older Spring examples (pre-6.1) show catching. `ApiExceptionHandler`
only handles the newer type.

**Do not add class-level `@Validated` to a controller just to validate its `@PathVariable`s.**
Spring MVC validates a constrained `@PathVariable`/`@RequestParam` natively since Framework 6.1,
with no class-level annotation required. Adding `@Validated` on the class instead activates the
older Spring AOP method-validation interceptor (`MethodValidationPostProcessor` /
`MethodValidationInterceptor`, the same mechanism `@Validated`-annotated `@Service` beans use) —
that interceptor wraps every call to the (now CGLIB-proxied) controller and throws a plain
`ConstraintViolationException` instead, bypassing the newer mechanism entirely. Confirmed by
running it: adding `@Validated` to `CatalogController` turned the intended `400` response bodies
empty, because `ApiExceptionHandler` had no handler for that exception type. Both controllers here
carry constraints directly on the parameter (`@PathVariable @NotBlank String sku`,
`@RequestParam @Email String customerEmail`) and no class-level `@Validated`.

**Validation groups — `catalog.dto.ProductRequest`.** `CreateProductRequest`/`UpdateProductRequest`
differed only in `sku` (immutable on update, sourced from the path). Rather than keep two
near-identical records, they were merged into one `ProductRequest`, with `sku` required only in the
`OnCreate` group:

```java
public record ProductRequest(
		@NotBlank(groups = OnCreate.class) String sku,
		@NotBlank String name,
		@Nullable String description,
		@Positive BigDecimal price,
		@NotBlank String categoryName) {
	public interface OnCreate extends Default {
	}

	public interface OnUpdate extends Default {
	}
}
```

`CatalogController` validates `createProduct` against `@Validated(OnCreate.class)` and
`replaceProduct` against `@Validated(OnUpdate.class)`. **The `extends Default` matters**: naming an
explicit group on `@Validated` *replaces* the implicit `Default` group rather than adding to it. The
first version of this omitted `extends Default`, and the create endpoint silently accepted a blank
`name` and a zero `price` — every unqualified constraint (the ones with no `groups` attribute, which
belong to `Default`) stopped being checked the moment `OnCreate` was named explicitly. Confirmed by
a failing slice test before the fix.

**Follow-up — the same treatment for categories.** `CreateCategoryRequest` was a single-purpose
DTO with no update counterpart, left inconsistent with the `ProductRequest` merge above. It was
replaced the same way: one `catalog.dto.CategoryRequest` (`name`, `description`), `name`
`@NotBlank(groups = OnCreate.class)`, both groups `extends Default`. This also meant adding the
endpoint that had been missing — `PUT /api/v1/categories/{name}`, which only ever changes
`description` (`name` is the natural key, sourced from the path, exactly like `sku` on
`replaceProduct`). `CatalogService.replaceCategory` is the new use case.

**Query-object validation.** `ProductFilter`'s `minPrice`/`maxPrice` carry `@PositiveOrZero`; the
controller's `@Valid` on the `@ParameterObject` parameter cascades into them — the query-parameter
counterpart to `@Valid` on a request body.

```bash
curl -i -X POST localhost:8080/api/v1/products -H 'Content-Type: application/json' \
  -d '{"sku":"X","name":"","price":-1,"categoryName":"Storage"}'
# HTTP/1.1 400, Content-Type: application/problem+json
# {"type":"about:blank","title":"Bad Request","status":400,"detail":"Invalid request content.",
#  "instance":"/api/v1/products","errors":[{"field":"name","message":"must not be blank"},
#  {"field":"price","message":"must be greater than 0"}]}
```

**Verification.** New slice cases: `CatalogControllerTest` (+3: invalid create body, blank path
variable, duplicate-sku 409 via the advice), `OrderControllerTest` (+2: empty lines + blank nested
address, invalid `@Email` filter). New IT cases: `CatalogApiIT` / `OrderApiIT` (+1 each) assert the
`application/problem+json` content type and `errors` shape end-to-end against real Oracle.
`./mvnw -pl spring-boot-training verify` → surefire 62 (+3 for the category follow-up:
`replaceCategoryReturns200`, `replaceUnknownCategoryReturns404`, `invalidCreateCategoryBodyReturns400WithFieldErrors`),
failsafe 59 (+1: `createThenReplaceCategoryRoundTrips`), BUILD SUCCESS.

### 4.3 DTOs

The wire contracts were already `*Request`/`*Response` records with no entity leakage (U4.1/U4.2).
This step adds the three things still missing: nested response DTOs for `Order`/`OrderItem`,
Jackson 3 shaping (`@JsonProperty`, a naming strategy, `@JsonView`), and a genuine partial-update
PATCH — `PATCH .../status` was always a single mandatory field, not "some fields provided, others
left alone".

**One response type, `@JsonView` picks the shape.** `order.dto.OrderResponse` (flat: `orderNumber`,
`status`, `total`, `lineCount`) is replaced by `OrderDetailResponse`, used by *every* order
endpoint. A `Views` holder (`Views.Summary`, `Views.Detail extends Summary`) tags each field;
`OrderController` puts `@JsonView(Views.Summary.class)` on `placeOrder`/`listOrders`/`updateStatus`
and `@JsonView(Views.Detail.class)` on `getOrder` — Spring MVC's built-in
`JsonViewResponseBodyAdvice` applies the filter directly from the handler-method annotation, no
manual `MappingJacksonValue` wrapping. One record, not two unrelated DTOs:

```java
public record OrderDetailResponse(
		@JsonView(Views.Summary.class) String orderNumber,
		@JsonView(Views.Summary.class) OrderStatus status,
		@JsonView(Views.Summary.class) @JsonProperty("totalAmount") BigDecimal total,
		@JsonView(Views.Summary.class) int lineCount,
		@JsonView(Views.Detail.class) @Nullable String customerEmail,
		@JsonView(Views.Detail.class) @Nullable OffsetDateTime placedAt,
		@JsonView(Views.Detail.class) @Nullable AddressResponse shippingAddress,
		@JsonView(Views.Detail.class) List<OrderLineResponse> lines) {
}
```

`OrderWebMapper.toResponse(OrderConfirmation)` (place/list/patch-status) only has the flat
confirmation to work with, so the four `Views.Detail` fields come back `null`/empty — harmless,
since a `Views.Summary`-filtered response never serializes them regardless of value.
`toDetailResponse(Order)` (get-by-number) has the whole aggregate — loaded via the new
`OrderRepository.findWithCustomerItemsAndProductsByOrderNumber` (`@EntityGraph` on `customer`,
`items`, and `items.product`, one round trip) — and fills everything, including the nested lines.

**Nested DTOs.** `order.dto.OrderLineResponse` (`sku`, `productName`, `quantity`, `unitPrice`,
`lineTotal`) and `order.dto.AddressResponse` mirror the request side's `OrderLineRequest`/
`AddressRequest`. `productName` flattens `item → product`, same reasoning as
`ProductResponse.categoryName`.

**Gotcha: `@JsonView` doesn't cascade to nested types on its own.**
`MapperFeature.DEFAULT_VIEW_INCLUSION` defaults to `false`, and that setting isn't scoped to the
root type — once any view is active for a response, *every* type reached during that same
serialization is filtered by it, not just the one carrying the `@JsonView`-annotated fields. The
first version of `AddressResponse`/`OrderLineResponse` had no `@JsonView` on their own fields at
all, on the assumption "unannotated means always included" (true for the *root* type, not for
anything nested under an active view) — the detail endpoint came back with `"shippingAddress":{}`
and `"lines":[{}]`, empty objects instead of real data. Fixed by tagging every field of both
records `@JsonView(Views.Detail.class)`. Caught by `OrderControllerTest` asserting an actual nested
field (`$.lines[0].sku`), not just that the container key was present.

**`@JsonProperty`.** `OrderDetailResponse.total` carries `@JsonProperty("totalAmount")` — the Java
name stays short, the wire name spells it out. Contrast the naming-strategy approach below, which
renames every field at once instead of one at a time.

**Naming strategy, scoped to one class.** `catalog.dto.ProductResponse` gets
`@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`: `categoryName` becomes
`category_name` on the wire, every other field is already one word. Applied per-class rather than
via `spring.jackson.property-naming-strategy` globally, so it reshapes exactly one, deliberately
chosen contract instead of silently changing every other DTO's already-documented JSON shape.

**Jackson 3 package split.** Verified against the actual `~/.m2` jars while building this:
`jackson-annotations` was **not** relocated — `@JsonProperty`/`@JsonView` stay on
`com.fasterxml.jackson.annotation.*` (still a 2.x artifact). `jackson-databind` **was** relocated —
`PropertyNamingStrategies` and `@JsonNaming` now live under `tools.jackson.databind.*` (a 3.x
artifact). Mixing the two up is a straightforward compile error, not a runtime surprise, but easy
to get wrong when porting a Jackson 2 example.

**Partial-update PATCH.** New `PATCH /api/v1/products/{sku}` + `catalog.dto.ProductPatchRequest` —
every field `@Nullable`; `CatalogService.patchProduct` applies only the non-null ones. No
validation groups needed: Jakarta Bean Validation already treats `null` as valid for everything
except `@NotNull`/`@NotBlank`/`@NotEmpty`, so "only validate what was actually provided" falls out
of the constraint annotations (`@Positive` on `price`) with no extra plumbing.

```java
public record ProductPatchRequest(
		@Nullable String name, @Nullable String description,
		@Nullable @Positive BigDecimal price, @Nullable String categoryName) {
}
```

*Known limitation, documented rather than worked around*: Jackson binds a genuinely absent JSON
field and an explicit `"field": null` to the same Java `null` — a plain record can't tell them
apart. This PATCH can replace `description` or leave it alone, but can never clear it back to
`null` once set (fall back to `PUT` for that). RFC 7396 JSON Merge Patch (`application/merge-patch+json`, deserialized as a `JsonNode` so
"absent" and "null" are
distinguishable) or RFC 6902 JSON Patch are the correct tools when that distinction matters —
deliberately out of scope here, the same "considered and documented, not built" treatment as the
4.2 validation-groups decision.

**Gotcha: a validated body sharing a method with a directly-constrained parameter reports the
wrong field name.** `patchProduct` was the first endpoint to combine a `@NotBlank @PathVariable`
with a `@Valid @RequestBody` on the *same* method (`replaceProduct` does too, but nothing had ever
exercised its body-validation failure path until now). That combination routes the body's cascaded
violations through `HandlerMethodValidationException`, not `MethodArgumentNotValidException` —
`config.web.ApiExceptionHandler` was reading every `ParameterValidationResult` as a flat scalar
violation and reporting the bare method-parameter name (`"request"`) instead of the actual field (`"price"`). Fixed by checking for
`org.springframework.validation.method.ParameterErrors` — the
subtype Spring uses for a cascaded object argument, itself a Spring `Errors` with real
`FieldError`s — and reading `getFieldErrors()` from it, the same as the `MethodArgumentNotValidException`
path already did. Caught by `CatalogControllerTest.patchProductWithANonPositivePriceReturns400`
expecting `"price"` and getting `"request"`; a new regression test (`invalidReplaceBodyReturns400WithFieldErrorsNotTheParameterName`) closes
the same gap on
`replaceProduct`.

```bash
curl -i localhost:8080/api/v1/products/LAP-0001            # category_name, not categoryName
curl -i -X PATCH localhost:8080/api/v1/products/LAP-0001 -H 'Content-Type: application/json' \
  -d '{"price":1199.00}'                                   # only price changes
curl -i localhost:8080/api/v1/orders                       # Summary view: totalAmount, no lines
curl -i localhost:8080/api/v1/orders/API-1                 # Detail view: nested lines + address
```

**Verification.** New slice cases: `CatalogControllerTest` (+4: PATCH happy path, PATCH validation
failure, PATCH unknown SKU, the `replaceProduct` field-name regression test),
`OrderControllerTest` (+1 new, 1 renamed: the Summary-view field-exclusion proof, the Detail-view
nested assertions). New IT cases: `CatalogApiIT` (+2: PATCH round-trip, PATCH validation failure);
`OrderApiIT`'s lifecycle test extended with Summary/Detail assertions in place.

### 4.4 Request / Response mapping

The U4.1 hand-written `@Component` mappers (`CatalogWebMapper`, `OrderWebMapper`) become MapStruct
`@Mapper(componentModel = "spring")` **interfaces** — no more hand-written implementation class.
MapStruct's annotation processor generates `CatalogWebMapperImpl` / `OrderWebMapperImpl` (each a
Spring `@Component`) at compile time from the interface's method signatures and `@Mapping`
annotations, under `target/generated-sources/annotations`. Both interfaces keep their U4.1 names
and public method signatures, so every call site (`CatalogService`, `OrderController`,
`OrderService`) is untouched — only the field type's *implementation* changed, same story as
swapping a repository's implementation without touching its callers.

**Deps.** `org.mapstruct:mapstruct` (compile) — version `mapstruct.version`, not in the Spring Boot
BOM. The annotation-processor path (`lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`,
order matters so Lombok's generated setters exist before MapStruct looks for them) was already
wired in the reactor parent's `maven-compiler-plugin` `pluginManagement`, ahead of this unit.

**Entity → DTO, with a nested property.** `CatalogWebMapper.toResponse(Product)` needs one
`@Mapping` — `categoryName` has no matching source property by name, so it is pulled from the
nested association:

```java

@Mapping(target = "categoryName", source = "category.name")
ProductResponse toResponse(Product product);
```

Every other field (`toResponse(Category)`, the top-level fields above) matches source and target
property names directly, so no annotation is needed at all — MapStruct wires those by convention.

**Nested + collection mapping, for free.** `OrderWebMapper.toCommand(PlaceOrderRequest)` carries **zero** `@Mapping` annotations.
`PlaceOrderCommand`'s components (`orderNumber`, `customerEmail`,
`shippingAddress`, `lines`) match `PlaceOrderRequest`'s by name; MapStruct auto-generates the
nested record conversion (`AddressRequest` → `Address`, identical component names) *and* the
collection conversion (`List<OrderLineRequest>` → `List<PlaceOrderCommand.Line>`, identical
component names) with no configuration — the cleanest illustration in this unit of what MapStruct
buys over hand-written mapping. `toDetailResponse(Order)` shows the same mechanism used
deliberately: `lines` (`Set<OrderItem>` → `List<OrderLineResponse>`) and `shippingAddress`
(`Address` → `AddressResponse`) are declared as ordinary `@Mapping(source = ...)` targets, and
MapStruct resolves the element/nested conversion by matching the return type against the other
abstract methods declared in the same interface (`toResponse(OrderItem)`, `toResponse(Address)`).

**`@MappingTarget` — update-entity-from-DTO.** This is what actually replaces hand-written code,
not just relocates it. `CatalogService.replaceProduct`/`patchProduct`/`replaceCategory` used to
set each entity field by hand (four `if (request.x() != null)` blocks for the PATCH case); they now
call a `void` mapper method that takes the DTO plus `@MappingTarget` and mutates the entity in
place:

```java

@Mapping(target = "category", ignore = true)
void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);

@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Mapping(target = "category", ignore = true)
void patchEntityFromRequest(ProductPatchRequest request, @MappingTarget Product product);
```

`nullValuePropertyMappingStrategy = IGNORE` generates the exact "skip if null" branch per field
that `CatalogService.patchProduct` used to write by hand. `category` is `ignore`d on both — the
request carries `categoryName` (`String`, the business key on the wire) but the entity field is a
`Category` (an association); MapStruct matches by *type* as well as name, so there is no automatic
`String → Category` conversion to generate, and resolving one needs a `CategoryRepository` query, a
service concern, not a mapper's. `CatalogService` still does that lookup and calls
`product.setCategory(...)` itself, immediately after the mapper call. `sku` (on `Product`) and
`name` (on `Category`) need no `ignore` at all: both are business keys with no setter (CLAUDE.md —
immutable, so `equals`/`hashCode` stay stable), and MapStruct only ever writes to a property that
has one.

**Two methods deliberately stay hand-written `default` methods**, because neither is really a
"map these matching properties" problem:

- `CatalogWebMapper.toResponsePage(Page<Product>)` — the `Page<Entity>` → `Page<Response>` helper
  the unit asks for. MapStruct has no `Page` support to generate against (it is a wrapper, not a
  bean with matching properties); the one-line `page.map(this::toResponse)` reuses the *generated*
  `toResponse(Product)` method underneath.
- `OrderWebMapper.toResponse(OrderConfirmation)` — three `OrderDetailResponse` fields are literal
  `null` and one is `List.of()`, none sourced from `OrderConfirmation` at all (it carries no
  customer email, placement time, address, or line detail). Forcing that through
  `@Mapping(constant = ...)` four times would read worse than the plain constructor call it
  replaces; safe regardless, since every caller (`placeOrder`/`listOrders`/`updateStatus`) is
  `@JsonView(Views.Summary.class)` and never serializes those fields.

**Computed fields via `expression`.** `OrderWebMapper.toResponse(OrderItem)` needs a `lineTotal`
that is not a property on anything — `unitPrice * quantity` — and `OrderItem.lineTotal()` (which
computes the same thing) is package-private in `order`, unreachable from `order.mapper`. Restated
as a MapStruct expression instead of a new public method:

```java

@Mapping(target = "sku", source = "product.sku")
@Mapping(target = "productName", source = "product.name")
@Mapping(target = "lineTotal",
		expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
OrderLineResponse toResponse(OrderItem item);
```

`toDetailResponse(Order)` uses the same technique for `lineCount`
(`expression = "java(order.getItems().size())"` — `Set.size()` isn't a bean property MapStruct can
match automatically) and plain `source = "..."` for the rest (`total ← totalAmount`,
`customerEmail ← customer.email`).

**Verifying the generated code.** `target/generated-sources/annotations/.../CatalogWebMapperImpl.java`
and `.../OrderWebMapperImpl.java` are worth reading once after a `compile` — every `@Mapping`
decision above shows up as plain, readable Java (null checks, field-by-field assignment, the
`if (request.x() != null)` guards), which is the actual selling point over a reflection-based
mapper: no runtime surprises, and a compile error the moment a mapped property stops matching.

**Test-slice fallout.** `OrderControllerTest` used to `@Import(OrderWebMapper.class)` (a concrete
`@Component` class); a `@WebMvcTest` slice does not component-scan `order.mapper`, and once
`OrderWebMapper` is an interface `@Import` needs a real class. Changed to
`@Import(OrderWebMapperImpl.class)` — MapStruct's generated implementation, by its standard
`<MapperName>Impl` name. `CatalogControllerTest` needed no change; it mocks `CatalogService`
directly and never touches `CatalogWebMapper`.

```bash
./mvnw -pl spring-boot-training compile
find spring-boot-training/target/generated-sources/annotations -name '*MapperImpl.java'
```

**Verification.** New unit tests, no Spring context (MapStruct's generated `@Component` still has a
public no-arg constructor): `catalog/mapper/CatalogWebMapperTest` (5 — nested `categoryName`, the
`Page` helper, both `@MappingTarget` update methods including the PATCH null-skip behaviour, the
category-request update writing only `description`), `order/mapper/OrderWebMapperTest` (3 — the
zero-`@Mapping` nested+collection `toCommand`, the `default`-method summary response, the full
`toDetailResponse` aggregate walk). `./mvnw -pl spring-boot-training verify` → surefire 75 (+8, no
regressions), failsafe 61 (unchanged — `CatalogApiIT`'s existing PATCH/PUT round-trips are the
end-to-end proof that the generated update methods behave exactly like the manual code they
replaced).

### 4.5 API testing

The last U4 topic closes three gaps the checklist names. Two were already covered by the time this
step started — `@WebMvcTest`/`MockMvcTester` slices per controller (`CatalogControllerTest`, 16
cases; `OrderControllerTest`, 8 cases), each with a happy path and several failure paths, built up
across U4.1–U4.4 — so this step adds only what both `CatalogApiIT`'s and `OrderApiIT`'s own Javadoc
had been pointing at all along: a RestAssured `*IT` against a real port, and an OpenAPI spec sanity
check.

**RestAssured vs. `MockMvcTester` — a real HTTP round trip, not an in-process dispatch.**
`MockMvcTester` (used by every `*ApiIT` through U4.4) never opens a socket — requests are dispatched
straight into the servlet container in the same JVM. `RestAssuredApiIT` is the counterpart that
does: `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` starts a real embedded Tomcat on
an OS-assigned port, `@LocalServerPort` captures it, and RestAssured's `given()/when()/then()` DSL
drives real HTTP requests against it — headers, status line, body, all serialized and parsed for
real. One class, not one per resource: `RANDOM_PORT` is a different Spring TestContext cache key from
every other `@SpringBootTest` in the module (all default `MOCK`), so this is the one place in the
suite that pays for a second, real-server context — combining the catalog and order happy paths into
one class means that cost is paid once. Every failure path is already covered by the `MockMvcTester`
tests, so duplicating them here through a different client would test the client, not the app.

```java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestAssuredApiIT extends AbstractOracleIntegrationTest {

	@LocalServerPort
	private int port;

	@BeforeEach
	void configureRestAssured() {
		RestAssured.port = port;
	}

	@Test
	void createThenGetProductRoundTrips() {
		given().contentType(ContentType.JSON).body(body)
				.when().post("/api/v1/products")
				.then().statusCode(201).header("Location", "http://localhost:" + port + "/api/v1/products/RA-1");
		// ...
	}
}
```

Redeclaring `@SpringBootTest` on the subclass (rather than only inheriting
`AbstractOracleIntegrationTest`'s default `MOCK` one) is the supported way to override just
`webEnvironment` — Spring resolves the *nearest* `@SpringBootTest` in the class hierarchy, so this
one wins outright, while the parent's `@ServiceConnection` Oracle field and `@ActiveProfiles("test")`
are still inherited (plain Java field/annotation inheritance, unaffected by which `@SpringBootTest`
governs the context).

**Dependency finding: REST Assured dropped out of the Spring Boot BOM.** Boot's dependencies BOM
manages `rest-assured.version` and imports `rest-assured-bom` through the 3.x line — confirmed by
grepping the actual `~/.m2` BOM poms — but **not** in 4.1.1; nothing in that generation's `pluginManagement`
mentions it at all. Pinned in the reactor the same way as `mapstruct.version`/`springdoc-openapi.version`:
`rest-assured.version = 6.0.1`, the version that "rounds out Jackson 3 support" on top of 6.0.0's
Spring 7 baseline — the right pin given this app is Jackson 3 end-to-end. Only the base
`io.rest-assured:rest-assured` module is pulled; `rest-assured-spring-mock-mvc` is not used (and
would not work here regardless — its `javax.servlet`-based bridge predates Spring Framework 7's move
to `jakarta.servlet`).

**OpenAPI spec sanity check, not a contract test.** `OpenApiSpecIT` asserts `GET /v3/api-docs` exists
and is shaped right — `$.info.title` matches `OpenApiConfig`'s document metadata, `$.tags[*].name`
lists `catalog`/`orders`, and `$.paths` has an entry for every resource this unit built (bracketed
JsonPath keys — `$.paths['/api/v1/products']` — because dot notation breaks on the `/` inside a path
key). A third test confirms `/swagger-ui.html` actually resolves to something (a `3xx` redirect to
`/swagger-ui/index.html`, confirmed by running the test rather than assumed). Deliberately not a full
contract test: restating every `@Operation`/`@ApiResponse` annotation as an assertion would break on
every harmless controller change for no real safety gained. Runs `@AutoConfigureMockMvc` on the
default `MOCK` web environment, so it reuses the same cached context as `CatalogApiIT`/`OrderApiIT` —
no extra Oracle container or context startup cost, unlike `RestAssuredApiIT`'s `RANDOM_PORT`.

```bash
./mvnw -pl spring-boot-training verify -Dit.test=RestAssuredApiIT,OpenApiSpecIT -DfailIfNoTests=false -Dsurefire.skip=true
```

**Verification.** New failsafe-only classes (both in the root test package — they span both
`catalog` and `order`, so neither belongs to one feature package): `RestAssuredApiIT` (2: product
create+get, order place+get), `OpenApiSpecIT` (3: document metadata, every resource's path present,
the Swagger UI redirect). `./mvnw -pl spring-boot-training verify` → surefire 75 (unchanged — no new
slice tests; that coverage already existed), failsafe 66 (+5), BUILD SUCCESS. This closes out U4.