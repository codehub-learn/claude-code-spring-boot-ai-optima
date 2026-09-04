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
Only unmanaged things (`mapstruct`, `spotless`, `jacoco`) carry an explicit `*.version`
property in the parent.

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

**Docker Compose / Postgres** (`spring-boot-docker-compose` + a `compose.yaml` Postgres
service) is deferred to U3.1, where the first real datasource lands and there is something
for it to wire.