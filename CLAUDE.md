# CLAUDE.md

Project-specific guidance for Claude Code. The user's global `~/.claude/CLAUDE.md` (workflow orchestration, task management, core
principles) still applies and is not repeated here.

## What this project is

A reference application that showcases the current Spring ecosystem for building AI-native REST services in Java:

- **Spring Boot 4.1.x** on **Spring Framework 7** (Jakarta EE 11, Jackson 3, JSpecify null-safety).
- **Spring AI 2.0.x** for chat, embeddings, RAG, tool calling, and vector search.
- **RDBMS**: PostgreSQL is the default (with `pgvector` as the Spring AI vector store). Oracle is a supported alternative; keep persistence
  code portable and push vendor specifics into config or migration scripts.
- **REST APIs** as the primary interface, documented with OpenAPI.
- **Maven** multi-module reactor (`gr.codelearn.<product>` groupId), **Java 25** (LTS) toolchain. See *Maven multi-module layout*.

The secondary goal is to use Claude Code to scaffold and evolve the codebase, so structure and conventions below are written to be
machine-followable.

## Build and run

Use the Maven wrapper (`./mvnw`, or `.\mvnw.cmd` on Windows PowerShell).

| Task                                      | Command                  |
|-------------------------------------------|--------------------------|
| Compile                                   | `./mvnw compile`         |
| Unit + slice tests                        | `./mvnw test`            |
| Full verify (integration tests, coverage) | `./mvnw verify`          |
| Run locally                               | `./mvnw spring-boot:run` |
| Format check                              | `./mvnw spotless:check`  |
| Apply format                              | `./mvnw spotless:apply`  |

Local infrastructure (Postgres + pgvector, the `grafana/otel-lgtm` telemetry stack, etc.) runs via Docker Compose; Spring Boot's
`spring-boot-docker-compose` module wires it automatically in dev. Grafana is at `http://localhost:3000`. Integration tests use
Testcontainers, so they need a running Docker daemon but no manual setup.

Never mark a task done until `./mvnw verify` and `spotless:check` pass.

## Maven multi-module layout

This repo is a Maven reactor that co-hosts multiple codebases (products) under one Git repo. Modules may depend on each other; the driver is
co-hosting. Three pom shapes exist: **parent** (`packaging = pom`, repo root), **aggregator** (`packaging = pom`, groups sibling modules),
and **code module** (`packaging = jar`; `spring-boot-maven-plugin` only on runnable application modules, library modules omit `<build>`).

Rules that apply to every pom:

- Tabs for indentation, CRLF, UTF-8, no final newline (`.editorconfig`); Spotless enforces.
- `<project>` attribute order: `xmlns:xsi`, then `xmlns`, then `xsi:schemaLocation`.
- Keep the `<!-- ... -->` banner comments and the single blank line between sections exactly as the skeletons show.
- `<name>` is always `[${project.artifactId}]`.
- Reactor `groupId` is `gr.codelearn.<product>`; child modules inherit `groupId` and `version` and declare neither.
- `<organization>` is always `Code.Learn by Code.Hub` / `https://www.codehub.gr/codelearn/`; SCM and distribution URLs sit under
  `github.com/codehub-learn/<repo>`.
- Skeletons are **structure only**. Never copy libraries or versions from them; pick dependencies and versions from the tech-stack table
  below and give each a short `<!-- ... -->` comment, grouping related entries with a blank line between groups.
- Native Boot structured logging is the choice (see *Observability*): do **not** exclude `spring-boot-starter-logging`, do **not** add
  Log4j2 / Disruptor, do **not** add a log4j version-ban enforcer rule.

When asked to "create the parent pom", "add an aggregator / module group", or "add a module / product / app", invoke the **`maven-scaffold`
skill** for the full skeletons and per-shape section order.

## Tech stack and library choices

Pick these unless there is a concrete reason not to. Flag deviations before implementing.

| Concern                          | Choice                                                                                                    | Notes                                                                                                             |
|----------------------------------|-----------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| Persistence                      | Spring Data JPA (Hibernate 7)                                                                             | Use Spring Data JDBC for simple aggregates if it fits better; decide per module.                                  |
| Schema migrations                | Flyway                                                                                                    | SQL-first, versioned scripts in `src/main/resources/db/migration`. Never rely on `ddl-auto` beyond `validate`.    |
| DTO <-> entity mapping           | MapStruct                                                                                                 | No hand-written mappers, no exposing entities from controllers.                                                   |
| Bean mapping for records         | MapStruct with `componentModel = "spring"`                                                                |                                                                                                                   |
| Validation                       | Jakarta Bean Validation (Hibernate Validator)                                                             | Validate at the controller boundary and on config properties.                                                     |
| API errors                       | RFC 9457 `ProblemDetail`                                                                                  | Central `@RestControllerAdvice`. No custom ad-hoc error JSON.                                                     |
| API docs                         | springdoc-openapi                                                                                         | Served at `/swagger-ui.html`; spec at `/v3/api-docs`.                                                             |
| Observability                    | Spring Boot Actuator + `spring-boot-starter-opentelemetry` (Micrometer, Micrometer Tracing, OTLP export)  | Actuator stays as the base; the starter adds auto-instrumentation and OTLP export of metrics, traces, and logs.   |
| Telemetry backend                | Grafana LGTM stack: Loki (logs), Grafana (dashboards), Tempo (traces), Mimir/Prometheus (metrics)         | Local dev via the `grafana/otel-lgtm` Docker Compose service, picked up by Spring Boot's Docker Compose support.  |
| Logging                          | Native Boot structured logging (`logging.structured.format.console`)                                      | No Logback/Logstash add-on. JSON logs correlated with `traceId`/`spanId`; Loki ingests via OTLP or Grafana Alloy. |
| Testing                          | JUnit 5, AssertJ, Spring Boot slice tests, Testcontainers, `RestAssured` or `MockMvcTester` for API tests |                                                                                                                   |
| Test data                        | Instancio or hand-built factories                                                                         | Avoid sharing mutable fixtures across tests.                                                                      |
| AI                               | Spring AI 2.0 starters (`spring-ai-starter-model-*`, `spring-ai-starter-vector-store-pgvector`)           | Use `ChatClient`, `Advisor`s, and `@Tool` methods. Keep prompts in `src/main/resources/prompts`.                  |
| Resilience for external/AI calls | Spring Retry + timeouts, Resilience4j if circuit breaking is needed                                       |                                                                                                                   |

Do not add a library that overlaps one already listed without discussing it first.

## Lombok vs records

Records are the default for any immutable data carrier. Reach for Lombok only where records cannot go.

- **Use records for**: request/response DTOs, API models, `@ConfigurationProperties` classes, value objects, MapStruct targets, event
  payloads, command/query objects, test fixtures.
- **Use Lombok for**: JPA `@Entity` classes only (they must be mutable, need a no-args constructor, and should not be records). Prefer
  `@Getter` / `@Setter` on the fields that need them,
  `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, and a builder where construction is complex. Do **not** put `@Data`,
  `@EqualsAndHashCode`, or `@ToString` on entities (proxy and lazy-loading hazards); write `equals`/`hashCode` by business key or use the id
  with care.
- No Lombok on anything that could be a record.
- No field injection anywhere. Constructor injection only; with a single constructor Spring needs no annotation, and Lombok's
  `@RequiredArgsConstructor` is fine on `@Service` / `@Component` classes.

## Package structure

Package by feature first, layer second. Root package: `com.giannacoulis.<app>`.

```
com.giannacoulis.<app>
├── <app>Application.java
├── config/                 # cross-cutting @Configuration, not feature-specific
│   ├── ai/                 # ChatClient, advisors, vector store wiring
│   ├── web/                # CORS, Jackson, ProblemDetail advice, OpenAPI
│   └── persistence/        # datasource, JPA, Flyway extras
├── common/                 # shared kernel: base types, error model, utilities
└── <feature>/              # one package per bounded feature, e.g. "chat", "document", "catalog"
    ├── <Feature>Controller.java        # REST layer, DTOs in/out only
    ├── <Feature>Service.java           # use-case orchestration, @Transactional here
    ├── <Feature>Repository.java        # Spring Data interface
    ├── <Feature>.java                  # JPA entity (Lombok)
    ├── dto/                            # records: <Feature>Request, <Feature>Response
    ├── mapper/                         # MapStruct mappers
    └── internal/                       # package-private implementation detail
```

Rules:

- A feature package owns its entities and repositories. Cross-feature access goes through the other feature's `Service`, never its
  repository or entity.
- Controllers never touch repositories or entities. Services never return entities to controllers.
- Keep classes package-private unless another package genuinely needs them. Public surface is a deliberate choice.
- Consider Spring Modulith to enforce these boundaries once there is more than one feature.

## Naming conventions

- **Packages**: singular, lowercase, feature-named (`document`, not `documents` or `docmgmt`).
- **Classes**: `PascalCase`, role suffix (`...Controller`, `...Service`, `...Repository`,
  `...Mapper`, `...Properties`, `...Config`, `...Exception`).
- **DTO records**: `<Feature><Action>Request` / `<Feature>Response` (e.g. `ChatCompletionRequest`,
  `DocumentResponse`). No `Dto` suffix.
- **Methods**: verb phrases; repository queries follow Spring Data derived-name grammar or use
  `@Query` with a clear name.
- **REST paths**: plural kebab-case nouns, `/api/v1/<resource>`; version in the path. Use HTTP verbs for actions, not verbs in the URL.
- **DB**: `snake_case` tables and columns, singular table names, `pk_`, `fk_`, `ix_`, `uq_` prefixes for constraints. Flyway files:
  `V<number>__<snake_case_description>.sql`.
- **Config properties**: kebab-case under an app-owned prefix (`app.<feature>.<key>`), bound to a record with `@ConfigurationProperties` and
  `@Validated`.
- **Test classes**: `<Type>Test` for unit/slice, `<Type>IT` for integration (Failsafe picks up `*IT`).
- **Constants**: `UPPER_SNAKE_CASE`; prefer enums or config over magic literals.

## Coding conventions

- Target Java 25 language features: records, pattern matching, sealed types, switch expressions, text blocks, virtual threads (enabled via
  `spring.threads.virtual.enabled=true`).
- Respect `.editorconfig`: tabs for indentation, max line length 140, UTF-8, CRLF, no final newline. Let Spotless enforce it.
- Honor JSpecify: packages are null-marked by default; annotate nullable returns/params explicitly.
- `@Transactional` lives on service methods, not controllers or repositories. Keep transactions short; do not call AI/network inside them.
- Prefer constructor-bound immutable collaborators and `final` fields.
- Throw domain exceptions from services; translate them to `ProblemDetail` in one place.
- No `System.out`; use SLF4J. No checked-exception wrapping without adding information.
- Every public REST endpoint: validated input, documented response, explicit status codes, a slice test, and at least one integration test
  for the happy path plus one failure path.

## Spring AI conventions

- Build `ChatClient` instances from a central config; do not `new` model clients in features.
- Put system/user prompt templates in `src/main/resources/prompts/*.st` and load them as `Resource`.
- Use `Advisor`s for RAG (`QuestionAnswerAdvisor`), memory, and logging rather than inlining that logic.
- Expose tools with `@Tool`-annotated methods on dedicated `*Tools` components; keep them side-effect-aware and well-described.
- Never log full prompts or completions at INFO in non-local profiles; they may contain user data.
- Keep API keys in environment variables / Spring config, never in code or committed properties. Local dev overrides go in
  `application-local.yml` (git-ignored) or `.env`.
- Vector store: `pgvector` via the Spring AI starter. Embedding dimension and distance metric are config, set once and migrated
  deliberately.

## Configuration and profiles

- `application.yml` holds shared defaults. Profile files: `application-local.yml`,
  `application-test.yml`, `application-prod.yml`.
- Secrets and machine-specific values come from the environment, not from committed files.
- Bind related properties into validated `@ConfigurationProperties` records; avoid scattering
  `@Value`.

## Observability

Actuator is **not** being replaced. It stays as the base; Spring Boot 4's
`spring-boot-starter-opentelemetry` layers Micrometer, Micrometer Tracing, and OTLP export on top so one starter covers all three signals.
Micrometer is still the internal facade; OTLP is the wire protocol; the Grafana LGTM stack is the backend.

- **Dependencies**: `spring-boot-starter-actuator` + `spring-boot-starter-opentelemetry`. Do not add
  `micrometer-registry-prometheus` unless a Prometheus pull endpoint is needed alongside OTLP push.
- **Actuator exposure**: in non-local profiles expose only `health`, `info`, `metrics`
  (and `prometheus` if used) on a separate management port. `health` drives liveness/readiness probes; `info` is filled from build + git
  metadata.
- **Metrics**: Micrometer to OTLP to Mimir/Prometheus. Set common resource attributes and tags (`service.name`, `service.version`,
  `deployment.environment`) once via `management.otlp.*` /
  `management.observations.key-values`.
- **Traces**: Micrometer Tracing bridge to OTLP to Tempo. Propagate W3C `traceparent`. HTTP in/out, JDBC, and Spring AI calls
  auto-instrument; wrap custom use-case logic with `@Observed` or an explicit `ObservationRegistry` span. Sample at 100% locally,
  ratio-based in prod.
- **Logs**: native Boot structured JSON logging, correlated with `traceId`/`spanId`, shipped to Loki via the OTLP appender or Grafana Alloy
  tailing stdout. No PII, and no full prompts or completions at INFO outside local (see the Spring AI section).
- **Spring AI**: enable `spring.ai.chat.observations.*` to capture model, token-usage, and latency metrics/spans. Keep prompt/response
  content capture off outside local.
- **Local stack**: `compose.yaml` runs a `grafana/otel-lgtm` container (Grafana UI on 3000, OTLP on 4317/4318). `application-local.yml`
  points `management.otlp.*` at it. Bring it up with
  `./mvnw spring-boot:run` (Docker running); dashboards at `http://localhost:3000`.
- **Prod**: the OTLP endpoint is an env var pointing at the real collector/gateway, never hardcoded.
- **Tests**: no telemetry backend; assert with `TestObservationRegistry` where behavior matters, and cover Actuator `health` with one
  integration test.

## Working agreement for Claude Code

- This is a greenfield repo. The first real task is scaffolding: `pom.xml`, the application class, base config, Docker Compose, and one
  vertical-slice feature that exercises DB + REST + Spring AI.
- Before generating multi-file structure, write the plan to `.claude/tasks` and confirm it (per the global workflow rules).
- When adding a feature, generate the full vertical slice (controller, service, repository, entity, DTO records, MapStruct mapper, Flyway
  migration, tests) in one pass, following the structure above.
- After any correction from the user, record the pattern in `.claude/tasks`.
- Prefer editing existing files over adding parallel variants. Keep changes minimal and scoped.
- Use the IntelliJ MCP (`idea` server) for build, inspection, and refactoring feedback when available.

## References

- [Spring Boot 4.1 release notes (InfoQ)](https://www.infoq.com/news/2026/06/spring-boot-4-1/)
- [Spring AI 2.0 GA (InfoQ Spring roundup)](https://www.infoq.com/news/2026/06/spring-news-roundup-jun08-2026/)
- [Spring AI 2.0 milestone announcement](https://spring.io/blog/2026/03/26/spring-ai-2-0-0-M4-and-1-1-4-and-1-0-5-available/)
- [Spring Boot Actuator observability reference](https://docs.spring.io/spring-boot/reference/actuator/observability.html)
- [OpenTelemetry with Spring Boot (Spring blog)](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/)