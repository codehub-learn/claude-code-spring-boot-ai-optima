# Task: Spring Boot training curriculum — code per thematic unit

Source scope: `docs/spring-boot-scope.md` (22 training hours, 4 thematic units). Spring AI scope is
separate and out of scope here — no AI code in this wave.

## Agreed decisions

- **Domain**: Catalog & Orders — `Category`, `Product`, `Customer`, `Order`, `OrderItem`, embedded
  `Address`, `OrderStatus` enum. Covers `@ManyToOne`, `@OneToMany`, `@ManyToMany`-via-join-entity,
  `@Embeddable`, enums.
- **Structure**: Hybrid. `catalog` vertical slice carries U3 + U4. U1 is the app bootstrap. U2
  framework mechanics live in a dedicated `foundation` demo package with small throwaway beans.
- **Modules**: single runnable application module.
- **Persistence**: JPA-primary for the domain; Spring Data JDBC / `JdbcClient` shown on a focused
  read-model / reporting slice only.
- **Naming**: reactor groupId is the flat `gr.codelearn` (never `gr.codelearn.<product>`); module
  artifactId `spring-boot-training`; root package `gr.codelearn.springbootai.training` (product
  segment lives in the package, not the groupId). CLAUDE.md + `maven-scaffold` skill (v1.1.0)
  corrected; the earlier `com.giannacoulis.<app>` value was a mistake.

## Pre-step

- [x] Scaffold the `spring-boot-training` application module via the `maven-scaffold` skill (runnable module: `spring-boot-maven-plugin`,
  register in parent `<modules>`), tab-aware
  Spotless Java config, `spring-boot-starter-webmvc` + `-actuator` baseline, compile + `spotless:check`.
  Done: `TrainingApplication` + minimal `application.yml` + context-loads test; `./mvnw verify`
  and `spotless:check` green. Deviations: (a) parent Spotless `<java>` config (indent-tabs +
  importOrder + removeUnusedImports + trimTrailingWhitespace, no opinionated formatter) was
  already tab-aware and passed — no change needed, the old "google-java-format fallback" note
  was stale. (b) Dropped the skeleton's `process-aot` execution: it refreshes the app context
  at build time and would break `package` once DB/AI wiring lands. Kept `layers` +
  `excludeGroupIds` + `mainClass`.

## U1 — Development Environment & Project Setup (2h)

- [x] **1.1 Spring Boot introduction** — `TrainingApplication` with `@SpringBootApplication`,
  `SpringApplication` customization (banner mode, listeners), a short doc/README on starters &
  auto-configuration, `--debug` auto-config report notes, custom `banner.txt`, Actuator
  `health` + `info` exposed, `spring.threads.virtual.enabled=true`.
- [x] **1.2 Maven** — the module pom itself as the teaching artifact (commented starters, build-info
	+ git-commit-info goals wired, `spring-boot:run` / `spring-boot:build-image` notes), profile
	  activation example.
- [x] **1.3 Project setup** — `application.yml` + `application-local.yml` + `application-test.yml`,
  `AppProperties` record (`@ConfigurationProperties("app")` + `@Validated`) with a nested block,
  native structured JSON console logging config, a `compose.yaml` Oracle service, DevTools note.

## U2 — Core Spring Framework Concepts (6h)

- [x] **2.1 Dependency injection** — `foundation.di` package: constructor injection baseline,
  `@Qualifier` + `@Primary` with two impls of one interface, inject `List<T>` and
  `Map<String,T>`, `ObjectProvider` for optional/lazy, `@Profile`-scoped bean, a commented
  circular-dependency example + how Boot reports it.
- [x] **2.2 Beans** — `foundation.beans` package: `@Configuration` with several `@Bean` methods,
  singleton vs `prototype` scope demo, lifecycle (`@PostConstruct` / `DisposableBean` /
  `@Bean(initMethod/destroyMethod)`), `@Lazy`, custom `@ConditionalOnProperty`, a `FactoryBean`,
  a `BeanPostProcessor` that logs bean creation.
- [x] **2.3 Components** — `foundation.components` package: stereotype + component-scan example,
  `@Value` vs `Environment` vs `@ConfigurationProperties` side by side, application events (`ApplicationEventPublisher`, `@EventListener`,
  async `@EventListener`,
  `@TransactionalEventListener`), a `@Scheduled` task, an `@Async` method, an AOP
  `@Aspect` that logs `@Service` method timing.

## U3 — Data Persistence Layer (8h)

- [x] **3.1 Datasource & migrations** — HikariCP config, Flyway `V1__create_catalog.sql` (+ later
  versions), `spring.jpa.hibernate.ddl-auto=validate`, Testcontainers Oracle base test class.
- [ ] **3.2 Spring Data JPA — model & associations** — `Category`, `Product`, `Customer`, `Order`,
  `OrderItem` entities (Lombok `@Getter/@Setter`, `@NoArgsConstructor(PROTECTED)`, business-key
  `equals`/`hashCode`, `@Version`), `@ManyToOne` / `@OneToMany` / `@ManyToMany` join entity,
  `@Embeddable Address`, `OrderStatus` enum, JPA auditing (`@CreatedDate` / `@LastModifiedDate`).
- [ ] **3.3 Spring Data JPA — repositories & queries** — repositories per aggregate, derived query
  methods, `@Query` JPQL + native, `Pageable` / `Sort`, `Specification` for dynamic filtering,
  `@EntityGraph` to fix N+1, interface + record (DTO) projections.
- [ ] **3.4 Transactions** — `@Transactional` on services, propagation (`REQUIRES_NEW`), rollback
  rules / `rollbackFor`, read-only, "no network/AI in a transaction" note, `@Transactional`
  self-invocation pitfall demo.
- [ ] **3.5 Spring Data JDBC / JdbcClient read-model** — a reporting slice (e.g. sales-per-category
  view) mapped with `JdbcClient`, one small aggregate with Spring Data JDBC, `JdbcClient` batch
  insert, a written note on JDBC-vs-JPA trade-offs.
- [ ] **3.6 Persistence testing** — `@DataJpaTest` for repositories, `@Sql` seed scripts,
  `@JdbcTest` for the read-model, a Testcontainers `*IT`, an explicit rollback-verification test.

## U4 — REST API Development (6h)

- [ ] **4.1 Controllers & REST endpoints** — `CatalogController` / `OrderController` under
  `/api/v1/...`, verb mappings, `@PathVariable` / `@RequestParam` / `@RequestBody` binding,
  `ResponseEntity` with explicit status codes (201 + `Location`, 204, etc.), pagination params,
  content negotiation, springdoc OpenAPI annotations + `/swagger-ui.html`.
- [ ] **4.2 Exception handling & validation** — Jakarta Bean Validation on request records,
  `@Validated` on path/query params and on `AppProperties`, validation groups for create vs
  update, central `@RestControllerAdvice` returning RFC 9457 `ProblemDetail`, domain exceptions (`ProductNotFoundException`, etc.) →
  `ProblemDetail` in one place.
- [ ] **4.3 DTOs** — `*Request` / `*Response` records per resource, no entity leakage, nested DTOs
  for `Order` + `OrderItem`, Jackson 3 shaping (`@JsonProperty`, naming strategy, `@JsonView`
  for list vs detail), a partial-update (PATCH) payload approach.
- [ ] **4.4 Request / Response mapping** — MapStruct mappers (`componentModel = "spring"`),
  entity ↔ DTO, nested + collection mapping, update-entity-from-DTO (`@MappingTarget`),
  `Page<Entity>` → `Page<Response>` helper.
- [ ] **4.5 API testing** — `@WebMvcTest` / `MockMvcTester` slice tests per controller (happy +
  one failure path each), RestAssured `*IT` against Testcontainers for the happy path, an
  OpenAPI spec sanity check.

## Definition of done (per topic and overall)

- `./mvnw verify` passes; touched files reformatted via the `idea` MCP (no Spotless — see lessons.md).
- Every REST endpoint: validated input, documented response, explicit status, one slice test,
  one happy-path IT, one failure-path test.
- Code follows CLAUDE.md conventions (records vs Lombok, package-by-feature, constructor injection,
  no entities in controllers).

## Review

### 1.1 Spring Boot introduction (done)

- `TrainingApplication`: `main` now builds a `SpringApplication` (`setBannerMode(CONSOLE)` +
  `addListeners(new StartupListener())`) instead of the static `run`. Switched from the
  scaffold's instance `void main` to `public static void main(String[])` for teaching clarity.
- `StartupListener implements ApplicationListener<ApplicationReadyEvent>` — logs app name +
  active profiles + `--debug` hint via SLF4J.
- `banner.txt` with `${AnsiColor.*}` + `${spring-boot.formatted-version}` +
  `${application.version}` (latter blank until 1.2 build-info).
- `application.yml`: added `info.app.{name,description}`, kept env info contributor; comments
  on the actuator + virtual-threads keys.
- `README.md`: 1.1 section — `@SpringBootApplication` breakdown, starters table,
  auto-config inspection (`--debug`, `/actuator/conditions`), banner, actuator, virtual threads.
- `ActuatorEndpointsTest` (`@SpringBootTest`, autowires `HealthEndpoint` + `InfoEndpoint`
  beans, no HTTP): health → `UP`, info → `app.name`.
- Boot 4 package moves discovered: `@AutoConfigureMockMvc` → `spring-boot-webmvc-test`
  (not transitive), `HealthEndpoint` → `spring-boot-health`, `TestRestTemplate` out of
  `spring-boot-test` core. MVC slice tests will need the extra dep in U4.5.
- `./mvnw -pl spring-boot-training verify` → 3 tests green; `spotless:check` clean.

### 1.2 Maven (done)

- `spring-boot-training/pom.xml`: `spring-boot-maven-plugin` gains a `build-info` execution (writes `META-INF/build-info.properties`);
  `git-commit-id-maven-plugin` added to `<plugins>`
  with no version/execution/config — all inherited from `spring-boot-starter-parent`'s
  pluginManagement (`revision` execution, `generateGitPropertiesFile`, v9.2.0). Starter block
  gains a comment explaining the BOM pins versions. New `<profiles>` section: `local`
  (`activeByDefault`) and `ci` (`<property><name>env.CI</name>`), each setting
  `spring-boot.run.profiles`.
- `application.yml`: `management.info.git.mode: full`, `management.info.build.enabled: true`,
  comments refreshed.
- `README.md`: 1.2 section — reactor + wrapper, starters/BOM version management,
  `spring-boot:run` (args, profiles), `spring-boot:build-image` (buildpacks, needs Docker),
  build-info + git-commit-id goal table, profile-activation styles + `activeByDefault` footgun,
  banner `${application.version}` = manifest `Implementation-Version` (jar only). Fixed the 1.1
  notes that implied build-info fills the banner.
- `ActuatorEndpointsTest`: new `infoEndpointExposesBuildAndGitGroups` asserting the `build`
  (`artifact`, `time`) and `git` (`branch`, nested `commit`) groups. Javadoc notes these need a
  full `./mvnw` run (both goals fire before `test`).
- Boot parent already manages `git-commit-id-maven-plugin` version + `revision` execution +
  `git.properties` config — module pom only switches it on.
- `./mvnw verify` → 4 tests green, `spotless:check` clean. `spotless:apply` also corrected
  `StartupListener` import order / continuation indent drifted by an IDE reformat.

### 1.3 Project setup (done)

- Deps: `spring-boot-starter-validation` (compile); `spring-boot-devtools`
  (`runtime`, `optional=true`).
- `config/AppProperties`: `record` + `@Validated` + `@ConfigurationProperties("app")`, nested
  `Catalog` (page-size guard-rails, `Duration cacheTtl`) and `Cors` records. `@DefaultValue` on
  the nested `Catalog` param so `app.catalog` can be omitted wholesale; `@Nullable description`
  for a genuinely optional scalar; `@NotBlank` / `@NotEmpty` / `@Positive` / `@Max` elsewhere.
  Registered via `@ConfigurationPropertiesScan` on `TrainingApplication`.
- `package-info.java` (`@NullMarked`) for the root package and `config` — JSpecify does not
  cascade, so one per package. `org.jspecify` is on the compile classpath via Spring Framework 7.
- `application.yml`: `app.*` tree; `logging.structured.format.console: ecs` +
  `logging.structured.ecs.service.{name,version,environment}` (version via `@project.version@`
  filtering). `application-local.yml`: `console:` empty (readable logs), app pkg `DEBUG`, all
  actuator endpoints, DevTools restart/livereload on. `application-test.yml`: banner off,
  `root=WARN`, DevTools off, test-only `app.*`.
- `ActuatorEndpointsTest` + `TrainingApplicationTests` get `@ActiveProfiles("test")`.
- `AppPropertiesTest`: `ApplicationContextRunner` — binding + defaults, `@NotBlank` failure,
  nested `@Positive` failure. No `@SpringBootTest`.
- Root `.gitignore`: added a `.env` / `.env.*` (keep `.env.example`) block for secrets; the
  rest (`target`, `.idea`, `*.log`, ...) was already there. `application-local.yml` stays
  committed by design.
- `README.md` 1.3 section: config-file table, typed-config rules, structured logging, DevTools,
  and the explicit Docker-Compose/Oracle deferral to U3.1.
- Verified: `./mvnw verify` → 7 tests green, `spotless:check` clean; ran the built jar and
  confirmed ECS JSON logs on stdout (`@timestamp`, `service.*`, `ecs.version`).
- Banner version follow-up: `${application.version}` was still blank under `spring-boot:run`
  (Boot's token needs the jar manifest). Switched `banner.txt` to `@project.version@` and added
  `**/banner.txt` to the filtered `<resources>` block in the reactor parent (excluded from the
  unfiltered block). Confirmed the version now renders under both `spring-boot:run` and the jar.
- Gotcha hit: a bare `@DefaultValue` on a `String` record component fails binding ("must have a non-empty default value"). Bare
  `@DefaultValue` is only for nested value-object
  params; use `@Nullable` for optional scalars. Recorded in lessons.

### 2.1 Dependency injection (done)

- New package `gr.codelearn.springbootai.training.foundation.di` (+ its own `@NullMarked`
  `package-info.java`; JSpecify does not cascade). No pom / module change — single runnable
  module, as agreed.
- `Notifier` interface + three package-private impls: `EmailNotifier` (`@Primary`, `@Order(10)`),
  `SmsNotifier` (`@Order(20)`), `ConsoleNotifier` (`@Profile("local")`, `@Order(30)`). `send()`
  returns `"<channel>:<message>"` so tests see which bean handled a call, no real I/O.
- `NotificationDispatcher` (`@Component`, single constructor, no `@Autowired`) consumes every
  style: bare `Notifier` → `@Primary`; `@Qualifier("smsNotifier")` overrides `@Primary`;
  `List<Notifier>` (sorted by `@Order`); `Map<String, Notifier>` (keyed by bean name);
  `ObjectProvider<ConsoleNotifier>` for the profile-gated optional bean. `@PostConstruct`
  `logWiring()` prints a one-line INFO summary on every boot.
- `CircularDependencyExample` — non-instantiable holder, Javadoc-only: the commented two-bean
  constructor cycle, Boot 4's fail-fast "form a cycle" report, and the fix ladder (break it / `@Lazy` / `allow-circular-references`).
- Tests (both `@SpringBootTest`, MOCK web env): `DependencyInjectionTest` (`@ActiveProfiles("test")`,
  5 cases) — primary/qualifier resolution, `List` order, `Map` keys, `ConsoleNotifier` absent +
  `ObjectProvider` empty. `DependencyInjectionLocalProfileTest` (`@ActiveProfiles({"test","local"})`,
  2 cases) — console bean joins list/map in order, `ObjectProvider` resolves.
- `README.md`: new `## U2` section with the 2.1 write-up (primary vs qualifier table, list/map,
  `ObjectProvider`, `@Profile`, circular-dependency note + run command).
- `./mvnw -pl spring-boot-training verify` → 14 tests green (was 7); `spotless:check` clean.
  The two `ConfigurationPropertiesBindException` WARNs in the log are `AppPropertiesTest`'s
  deliberate validation-failure cases, not new.

### 2.2 Beans (done)

- New package `gr.codelearn.springbootai.training.foundation.beans` (+ `@NullMarked`
  `package-info.java`). `BeansConfig` (`@Configuration`) hosts the `@Bean`-method demos; a few
  supporting types are plain `@Component`s where a `@Bean` method can't show the point.
- Scope: `Widget` via `standardWidget()` (singleton, default) and `customWidget()`
  (`@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)`) — same class, two lifecycles.
- Lifecycle, three styles on three classes: `AnnotationLifecycleBean`
  (`@PostConstruct`/`@PreDestroy`), `InterfaceLifecycleBean` (`DisposableBean.destroy()`),
  `ThirdPartyResource` (`@Bean(initMethod = "start", destroyMethod = "stop")` — the
  "can't-annotate-a-library-class" case).
- `LazyGreeter` (`@Lazy` on `BeansConfig.lazyGreeter()`) — a static construction counter proves
  it isn't built until first `getBean`.
- `PremiumSupportChannel` gated by `@ConditionalOnProperty(prefix = "app.foundation",
  name = "premium-support", havingValue = "true")` — not wired into `AppProperties`, a raw
  environment property is enough for the demo.
- `WidgetFactoryBean implements FactoryBean<Widget>` — `@Component`-scanned; `getBean("widgetFactoryBean")`
  returns the product, `getBean("&widgetFactoryBean")` the factory itself.
- `LoggingBeanPostProcessor` (`@Component implements BeanPostProcessor`) — logs
  `postProcessAfterInitialization` for beans under `gr.codelearn.springbootai.training`, keeps an
  instance list (`observedBeanNames()`) for tests instead of static/shared state.
- Tests, all `ApplicationContextRunner` (no full `@SpringBootTest`, matching `AppPropertiesTest`'s
  isolated-context style): `BeanScopeTest` (2), `BeanLifecycleTest` (3, captures each bean via
  `AtomicReference` and asserts the destroy flag after the runner closes the context),
  `LazyBeanTest` (1, before/after construction-count delta), `ConditionalBeanTest` (3: absent /
  `=true` / `=false`), `WidgetFactoryBeanTest` (2: plain name vs `&`-prefixed name),
  `LoggingBeanPostProcessorTest` (1: eager singletons observed, untouched prototype/`@Lazy` beans
  not).
- `README.md`: new `### 2.2 Beans` section — scope table isn't needed, lifecycle-styles table,
  and one paragraph per concept.
- `./mvnw -pl spring-boot-training verify` → 26 tests green (was 14); `spotless:apply` then
  `spotless:check` clean.

### 2.3 Components (done)

- New package `gr.codelearn.springbootai.training.foundation.components` (+ `@NullMarked`
  `package-info.java`). `ComponentsConfig` (`@Configuration @EnableAsync @EnableScheduling`) turns on
  async + scheduling context-wide — every `@SpringBootTest` now has them; kept benign (one cheap
  `@Scheduled` counter, long default interval).
- Stereotypes: `MessageProvider` with `StaticMessageComponent` (`@Component`), `GreetingService`
  (`@Service`, also the aspect target), `MessageRepositoryStub` (`@Repository`). `StereotypeInventory`
  injects `List<MessageProvider>` and resolves each bean's stereotype via `AnnotatedElementUtils` +
  `ClassUtils.getUserClass` (unwraps the CGLIB proxy on `GreetingService`).
- `ConfigAccessDemo`: `app.name` read three ways — `@Value`, `Environment#getProperty`,
  `AppProperties#name()` (reuses the existing 1.3 record; adds a `components` -> `config` package ref).
- Events: `GreetingEvent` (record), `GreetingEventPublisher` (`ApplicationEventPublisher`),
  `GreetingEventListener` with `@EventListener` (sync), `@Async @EventListener` (different thread,
  `CountDownLatch` seam), and `@TransactionalEventListener(AFTER_COMMIT)` — wired but dormant (no
  transaction manager until U3.1; live demo deferred to U3.4, per the `CircularDependencyExample`
  precedent). `@Async` handler methods made `public` for reliable CGLIB proxying.
- `HeartbeatTask`: `@Scheduled(fixedDelayString = "${app.foundation.heartbeat-interval:PT1H}")`;
  `application.yml` sets `1h` (one beat at startup), `application-test.yml` sets `200ms`. Key is not
  bound by `AppProperties` (no `foundation` field; binder ignores unknown keys) — read straight from
  the annotation.
- `AsyncGreeter.greetAsync` → `@Async CompletableFuture<String>`.
- `ServiceTimingAspect`: `@Aspect @Around` logging elapsed millis, pointcut scoped to `@Service` beans
  `within(..foundation.components..*)` so it will not catch U3/U4 services. Keeps `lastTiming()` for
  the test.
- Deps: Boot 4 has no `spring-boot-starter-aop` — it was renamed `spring-boot-starter-aspectj`
  (`spring-aop` + `aspectjweaver`, no `spring-aspects` / LTW). Added `spring-tx` explicitly too, only
  so `@TransactionalEventListener` compiles; U3.1's `spring-boot-starter-data-jpa` will bring it
  transitively. Recorded both in lessons.
- Tests (all `@SpringBootTest @ActiveProfiles("test")`, one shared context): `ComponentStereotypeTest`
  (3), `ConfigAccessDemoTest` (1), `ApplicationEventsTest` (2), `SchedulingAndAsyncTest` (2),
  `ServiceTimingAspectTest` (2).
- IDE-vs-Spotless drift again (lessons.md): the open IDE reformatted five untouched files (`StartupListener`, `NotificationDispatcher`,
  `AppPropertiesTest`, `ConditionalBeanTest`,
  `LoggingBeanPostProcessorTest`) with tab+space continuation indent. `spotless:apply` reverted them
  to tabs-only; the diffs are whitespace-only (`git diff -w` is empty).
- `./mvnw -pl spring-boot-training verify` → 36 tests green (was 26); `spotless:check` clean.

### 3.1 Datasource & migrations (done)

- Deps (`spring-boot-training/pom.xml`): `spring-boot-starter-data-jpa`, `ojdbc17` (runtime),
  `spring-boot-flyway` + `flyway-database-oracle`, test: `spring-boot-testcontainers` +
  `testcontainers-oracle-free` + `testcontainers-junit-jupiter`. `maven-failsafe-plugin` switched on (inherits executions/config from the
  Boot parent) so `*IT` runs.
- Boot 4 gotcha: `FlywayAutoConfiguration` lives in the `spring-boot-flyway` *module*, not `flyway-core`.
  With only the raw `flyway-core` jar the app starts but never migrates (empty schema, no
  `flyway_schema_history`). Depend on `org.springframework.boot:spring-boot-flyway`. Lessons updated.
- Testcontainers 2.0 gotcha: every module artifact was renamed with a `testcontainers-` prefix (`org.testcontainers:oracle-free` ->
  `testcontainers-oracle-free`, `junit-jupiter` ->
  `testcontainers-junit-jupiter`). The bare names fail with "version is missing" (not in the 2.0 BOM).
  Container class is `org.testcontainers.oracle.OracleContainer`. Lessons updated.
- `compose.yaml` at repo root: a `gvenzl/oracle-free:23-slim` service (Oracle Database 23ai Free, slim).
  Env `ORACLE_PASSWORD` (SYS/SYSTEM) + `APP_USER`/`APP_USER_PASSWORD` = `training`/`training`
  (auto-created in the `FREEPDB1` PDB). Oracle-container best practice: `shm_size: 1g` (mandatory -
  ORA-27101 on Docker's 64 MB `/dev/shm`), the image's `healthcheck.sh` with a long `start_period`,
  named `oracle-data` volume at `/opt/oracle/oradata`, first-boot init hook `./compose/oracle-init` ->
  `/container-entrypoint-initdb.d` (+ a `.gitkeep`), `init: true`.
- `application.yml`: `spring.datasource.url` = `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` +
  `training`/`training` (the `compose.yaml` values, a throwaway local credential; prod overrides with
  `SPRING_DATASOURCE_*` env). `spring.datasource.hikari.*` pool tuning, `spring.jpa` (`ddl-auto:
  validate`, `open-in-view: false`, `format_sql`), `spring.flyway` (`enabled: false`, `locations`,
  `baseline-on-migrate`). Hibernate dialect stays auto-detected. `application-local.yml`:
  `org.hibernate.SQL` DEBUG / `orm.jdbc.bind` TRACE / `flywaydb` DEBUG. `application-test.yml`:
  `spring.flyway.enabled: true`, `format_sql: false`.
- Flyway is manual, not on boot: `spring.flyway.enabled: false` in `application.yml`; a
  `flyway-maven-plugin` in the module `pom.xml` (no lifecycle execution) runs
  `./mvnw -pl spring-boot-training flyway:migrate|info|validate|clean` against the running Oracle (plugin deps: `ojdbc17` +
  `flyway-database-oracle`, versions from the BOM props
  `${oracle-database.version}` / `${flyway.version}`; connection defaults match `compose.yaml`,
  override with `-Dflyway.*`). The `test` profile is the exception - `application-test.yml` sets
  `spring.flyway.enabled: true` so Boot migrates the per-run Testcontainers DB (which the plugin
  cannot target) on context start; `spring-boot-flyway` is kept for exactly that.
- `spring-boot-docker-compose` is not used (keeps boot fast) - start the DB yourself with
  `docker compose up -d` before `spring-boot:run` / `flyway:migrate`. Tests are unaffected:
  Testcontainers `@ServiceConnection` supplies its own `JdbcConnectionDetails`.
- `db/migration/V1__create_catalog.sql`: `category` + `product` in Oracle syntax (`number(19)` identity
  PKs, `varchar2`, `number(12,2)`, `timestamp with time zone default systimestamp`, `default` before
  `not null`; `uq_`/`fk_`/`ix_`/`ck_` constraints; `created_at`/`updated_at`/`version` columns for the
  3.2 entities). DDL only.
- Tests: `support/AbstractOracleIntegrationTest` - `@SpringBootTest @ActiveProfiles("test")` + a
  singleton `@ServiceConnection OracleContainer` (`gvenzl/oracle-free:23-slim`, static field,
  `static { start(); }`, no `@Testcontainers`). All 10 `@SpringBootTest` classes `extends` it (a
  datasource on the classpath means a full context needs a DB); the `ApplicationContextRunner` tests
  are untouched. `DependencyInjectionLocalProfileTest` keeps `@ActiveProfiles("local")` (merges to
  `{test, local}`).
- `persistence/DatasourceMigrationIT` (`extends` the base, `JdbcClient`): asserts the V1 row in
  `"flyway_schema_history"` (quoted lower-case on Oracle; `"success"` read as int), `CATEGORY`/`PRODUCT`
  in `user_tables` (unquoted DDL names fold UPPER-case), and `FK_PRODUCT_CATEGORY` in `user_constraints`
  (`constraint_type = 'R'`).
- `README.md`: `## U3` + `### 3.1` section. `CLAUDE.md`: RDBMS bullet, local-infra line, AI vector-store
  row/bullet all name Oracle. Memory + lessons.md updated.
- IDE-vs-Spotless drift (lessons.md) hit again: the open IDE tab+space-aligns wrapped call args that
  Spotless wants tabs-only. `spotless:apply` fixes it, but the IDE re-breaks it between `apply` and
  `check` - run `spotless:apply` then `verify` with nothing touching files in between.
- `./mvnw -pl spring-boot-training verify` -> BUILD SUCCESS, `spotless:check` clean, Failsafe `*IT` 3/3 (Testcontainers Oracle up in ~24 s,
  `DatasourceMigrationIT` green against real Oracle).

### 3.1 - sample data seeding (follow-up, done)

Not a new checklist item (per `lessons.md`, the user owns the checklist). A follow-up to 3.1: the
schema existed but no data was loaded. Plan: `.claude/plans/graceful-hugging-wilkes.md`. User
decisions - two-tier data, repeatable (`R__`) seeds, pure-SQL bulk generation.

- **Two-tier, Flyway-owned, SQL-first.** Baseline seed `db/migration/R__010_seed_catalog.sql`
  (repeatable): one `MERGE` per table on the natural key (`category.name`, `product.sku`), 6
  categories + 24 products, identity `id` never inserted, `created_at`/`updated_at`/`version` left
  to DDL defaults. Lives in `db/migration`, so the `test` profile's Boot-managed Flyway applies it
  right after `V1` - every `@SpringBootTest` now starts from these rows.
- **Demo bulk** `db/demo/R__910_demo_catalog.sql` (repeatable): a `begin dbms_random.seed(4242); end; /`
  block then `delete ... where sku like 'GEN-%'` + `insert ... select ... connect by level <= 500`,
  categories assigned by `row_number()` so it does not assume contiguous ids. Opt-in only.
- **Maven wiring.** New `${flyway.locations}` property in `spring-boot-training/pom.xml` (default =
  `db/migration`); the flyway-maven-plugin `<configuration>` drops its hard-coded `<locations>` and
  reads the property. New `demo` profile overrides the property to append `db/demo`. So
  `./mvnw -pl spring-boot-training flyway:migrate -Pdemo` loads baseline + ~500 generated products;
  plain `flyway:migrate` loads baseline only; `-Dflyway.cleanDisabled=false flyway:clean flyway:migrate`
  resets to baseline (Flyway 10+ disables `clean` by default).
- **Test.** `persistence/CatalogSeedIT` (`extends AbstractOracleIntegrationTest`, `JdbcClient`):
  baseline counts >= 6 / >= 24, no orphan `category_id`, zero `GEN-%` rows in the test DB.
  `DatasourceMigrationIT` unaffected (its queries filter by `"version" = '1'`).
- **Docs.** `README.md` 3.1 gains a "Sample data - baseline vs demo" subsection (tier table +
  commands + the "extend the `R__0*` seed when you add a `V*` DDL" rule). `CLAUDE.md`: a seeding
  bullet under *Working agreement* + a pointer on the *Schema migrations* table row. `lessons.md` +
  auto-memory updated.
- **Deferred to 3.2** (needs the `V2` orders tables + entities): `R__020_seed_orders.sql` baseline
  and `db/demo/R__920_demo_orders.sql` bulk, following this same convention.
- Verified: `./mvnw -pl spring-boot-training verify` green (surefire 39, failsafe 6/6 - `CatalogSeedIT`
  3/3 new, `DatasourceMigrationIT` 3/3 unchanged), `spotless:check` clean. Manual round-trip against
  compose Oracle: baseline = 6 categories / 24 products / 0 orphans; `-Pdemo` = 524 products (500 `GEN-%`); clean + migrate back to 24.
- Reverted a one-line comment added to `V1__create_catalog.sql` - editing an applied versioned
  migration tripped Flyway checksum validation on an already-migrated DB. The R__/V pairing note
  lives in `R__010`'s header and the docs instead. (Lesson: never touch an applied `V*` file.)

Follow-up (2026-09-10): the `-Pdemo` opt-in was removed. Reason: (a) after one `-Pdemo` run a plain
`flyway:migrate` aborts with `Detected applied migration not resolved locally: 910 demo catalog`
(Flyway 12 `validate` treats a tracked repeatable missing from the path as a hard error), and (b)
local dev runs the app from the IDE and wants the full dataset - only the Testcontainers tests need
the small baseline. Now `db/demo` is a permanent `<location>` on the flyway-maven-plugin (the
`${flyway.locations}` property was dropped - a long one-line CSV trips the 140-col limit and IDEA
then wraps it, which the plugin rejects). `./mvnw flyway:migrate` applies both tiers; the tests
still get baseline only via Boot's `spring.flyway.locations: classpath:db/migration`. `demo` Maven
profile deleted; docs + lessons corrected.