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
  native structured JSON console logging config, `spring-boot-docker-compose` wiring to a
  Postgres + pgvector-less Postgres service in `compose.yaml`, DevTools note.

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

- [ ] **3.1 Datasource & migrations** — HikariCP config, Flyway `V1__create_catalog.sql` (+ later
  versions), `spring.jpa.hibernate.ddl-auto=validate`, Testcontainers Postgres base test class.
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

- `./mvnw verify` and `./mvnw spotless:check` pass.
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
  and the explicit Docker-Compose/Postgres deferral to U3.1.
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