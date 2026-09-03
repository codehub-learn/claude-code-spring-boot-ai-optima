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
  Spotless Java config, `spring-boot-starter-web` + `-actuator` baseline, compile + `spotless:check`.
  Done: `TrainingApplication` + minimal `application.yml` + context-loads test; `./mvnw verify`
  and `spotless:check` green. Deviations: (a) parent Spotless `<java>` config (indent-tabs +
  importOrder + removeUnusedImports + trimTrailingWhitespace, no opinionated formatter) was
  already tab-aware and passed — no change needed, the old "google-java-format fallback" note
  was stale. (b) Dropped the skeleton's `process-aot` execution: it refreshes the app context
  at build time and would break `package` once DB/AI wiring lands. Kept `layers` +
  `excludeGroupIds` + `mainClass`.

## U1 — Development Environment & Project Setup (2h)

- [ ] **1.1 Spring Boot introduction** — `TrainingApplication` with `@SpringBootApplication`,
  `SpringApplication` customization (banner mode, listeners), a short doc/README on starters &
  auto-configuration, `--debug` auto-config report notes, custom `banner.txt`, Actuator
  `health` + `info` exposed, `spring.threads.virtual.enabled=true`.
- [ ] **1.2 Maven** — the module pom itself as the teaching artifact (commented starters, build-info
	+ git-commit-info goals wired, `spring-boot:run` / `spring-boot:build-image` notes), profile
	  activation example.
- [ ] **1.3 Project setup** — `application.yml` + `application-local.yml` + `application-test.yml`,
  `AppProperties` record (`@ConfigurationProperties("app")` + `@Validated`) with a nested block,
  native structured JSON console logging config, `spring-boot-docker-compose` wiring to a
  Postgres + pgvector-less Postgres service in `compose.yaml`, DevTools note.

## U2 — Core Spring Framework Concepts (6h)

- [ ] **2.1 Dependency injection** — `foundation.di` package: constructor injection baseline,
  `@Qualifier` + `@Primary` with two impls of one interface, inject `List<T>` and
  `Map<String,T>`, `ObjectProvider` for optional/lazy, `@Profile`-scoped bean, a commented
  circular-dependency example + how Boot reports it.
- [ ] **2.2 Beans** — `foundation.beans` package: `@Configuration` with several `@Bean` methods,
  singleton vs `prototype` scope demo, lifecycle (`@PostConstruct` / `DisposableBean` /
  `@Bean(initMethod/destroyMethod)`), `@Lazy`, custom `@ConditionalOnProperty`, a `FactoryBean`,
  a `BeanPostProcessor` that logs bean creation.
- [ ] **2.3 Components** — `foundation.components` package: stereotype + component-scan example,
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

_(filled in as topics are completed)_