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

| Annotation                 | Effect                                                              |
|----------------------------|---------------------------------------------------------------------|
| `@SpringBootConfiguration` | marks the class as the primary `@Configuration` source              |
| `@EnableAutoConfiguration` | activates condition-driven auto-configuration                       |
| `@ComponentScan`           | scans this package (`gr.codelearn.springbootai.training`) and below |

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

| Method | Setting | Point |
|---|---|---|
| `placeOrder` | `@Transactional` | baseline read-write tx; saves the graph, publishes `OrderPlacedEvent` |
| `findOrder` | `readOnly = true` | Hibernate flush mode → `MANUAL`, connection flagged read-only; use it for every query method |
| `peekStatusAfterLocalEdit` | `readOnly = true` | mutates a loaded entity and returns the new value, but the read-only tx **never flushes** — reload and the row is unchanged. A dirty write here is silently lost, not rejected |
| `placeOrderAndDeliverReceipt` | `rollbackFor = ReceiptDeliveryException.class` | the default rule **commits** on a checked exception; `rollbackFor` opts in to rollback |
| `placeOrderToleratingLowStock` | `noRollbackFor = LowStockException.class` | the default rule **rolls back** on a `RuntimeException`; `noRollbackFor` opts out, so the order commits and the exception still propagates |
| `placeBatchIndependently` | *(none)* — delegates to `REQUIRES_NEW` | each order commits in its own transaction (via `OrderPlacementService`), so one bad order does not undo the ones already placed |
| `placeBatchAtomically` | `@Transactional` | the contrast: one transaction for the batch, first bad order rolls back the lot |
| `probeTransactionActive` | `@Transactional` | returns `TransactionSynchronizationManager.isActualTransactionActive()` |

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

**Verification.** `OrderServiceIT` (8 tests) runs against real Oracle and is **not**
`@Transactional` (a test-managed tx would mask commit / `REQUIRES_NEW` / rollback semantics);
it uses `TX-`-prefixed order numbers and deletes them in `@AfterEach`. `./mvnw -pl
spring-boot-training verify` → surefire 36, failsafe 33 (+8), all green.