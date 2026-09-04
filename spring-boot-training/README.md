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
`${AnsiColor.*}` for colour and `${spring-boot.formatted-version}` / `${application.version}`
for versions. `application.version` stays blank under `spring-boot:run` until topic 1.2 wires
`build-info`.

**Actuator `health` / `info`.** `application.yml` exposes only `health,info` over HTTP,
sets `health.show-details: always`, and enables the env `info` contributor so the static
`info.app.*` tree surfaces at `GET /actuator/info`. Build and git metadata arrive in 1.2.

**Virtual threads.** `spring.threads.virtual.enabled: true` makes request handling and
`@Async` run on JDK virtual threads.