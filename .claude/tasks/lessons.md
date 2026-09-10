# Lessons

Patterns captured after user corrections. Review at session start.

## todo.md: never edit the checklist item text

- In `.claude/tasks/todo.md`, the `- [ ]` items are the plan. Only ever flip `[ ]` -> `[x]` when a topic
  is complete. Do **not** append "Done: ..." / "Deviations: ..." to the item body — that goes in the
  `## Review` section as a `### <topic> (done)` block. User correction, 2026-09-04.
- Never add a new checklist step on your own. The user adds steps (e.g. "1.4") explicitly; when asked,
  ask the clarifying questions needed to pin down its scope before writing it into the plan.
- New `### <topic> (done)` blocks are **appended at the end of the `## Review` section**, in completion
  order (so 2.1 goes after 1.3, not right under the `## Review` heading). User correction, 2026-09-04.

## Toolchain / version pins

- **Never guess a patch version for a pinned toolchain.** When proposing the Maven wrapper version I picked
  `3.9.9`; the user corrected it to `3.9.16`. For anything the user will run (Maven wrapper, JDK, DB image
  tags), surface the exact version as a question or use what is already on the machine, do not invent a
  plausible-looking one.

## Spring Boot parent plugin management

- `spring-boot-starter-parent` 4.1.x manages versions for `maven-enforcer-plugin`, `maven-compiler-plugin`,
  surefire/failsafe, `spring-boot-maven-plugin`. It does **not** manage `jacoco-maven-plugin` or
  `spotless-maven-plugin` — declare their versions via a `*.version` property.
- It also fully preconfigures `io.github.git-commit-id:git-commit-id-maven-plugin` (v9.2.0 in 4.1.1):
  version, a `revision` execution, and `generateGitPropertiesFile` → `target/classes/git.properties`.
  To use it, just list the plugin (groupId + artifactId, no version, no execution) in the module
  `<plugins>`. `spring-boot-maven-plugin` only gets a `repackage` execution from the parent — add a
  `build-info` execution explicitly if you want `/actuator/info` build data.

## groupId is flat `gr.codelearn`

- The reactor `groupId` is always the flat `gr.codelearn`, never `gr.codelearn.<product>` or
  `gr.codelearn.<app>`. The product name lives in the `artifactId` and the Java package (`gr.codelearn.<product>.<app>`), not the `groupId`.
  I first wrote `gr.codelearn.springbootai`
  as the groupId and CLAUDE.md said `com.giannacoulis.<app>` for the root package; both were
  wrong. Fixed in `pom.xml`, `spring-boot-training/pom.xml`, `CLAUDE.md`, and the `maven-scaffold`
  skill (v1.1.0).

## Formatting: IntelliJ only, no Spotless (resolved 2026-09-10)

- **Spotless is removed.** It fought IntelliJ endlessly: `ij_java_align_multiline_parameters_in_calls = true`
  aligns wrapped call args with `tab+space`, Spotless `<indent><tabs>` forces tabs-only, and the open IDE
  re-broke every `spotless:apply`. No Maven Java formatter implements the `ij_*` keys, so a true sync was
  never possible. User decision: `.editorconfig` is the single source of truth; formatting is applied by
  IntelliJ via the `idea` MCP `reformat_file` tool; there is no build-time formatting gate.
- **Rule (CLAUDE.md → Formatting):** after creating or editing any file, reformat it through the `idea`
  MCP before finishing. Do not hand-align continuations. If the MCP is unavailable, leave formatting to
  the user.
- Removed from the parent `pom.xml`: the `spotless-maven-plugin` (both `<pluginManagement>` and
  `<plugins>` entries) and the `spotless-maven-plugin.version` property. `verify` no longer runs a
  format check.

## Boot 4 web starter name

- Use `spring-boot-starter-webmvc`, not `spring-boot-starter-web` (the 4.1 POM marks the latter
  deprecated). Recorded in CLAUDE.md tech-stack table and `maven-scaffold` v1.1.1.

## Boot 4 AOP starter renamed; spring-tx not on the classpath by default

- There is **no `spring-boot-starter-aop` in the Boot 4.1 BOM** — adding it fails with
  "'dependencies.dependency.version' ... is missing". It was renamed **`spring-boot-starter-aspectj`**
  (pulls `spring-aop` + `aspectjweaver`, no `spring-aspects`, no load-time weaving). `aspectjweaver` on
  the classpath is what activates Boot's AOP auto-config / `@EnableAspectJAutoProxy`.
- `spring-tx` is **not transitively present** with just webmvc + actuator + validation + aspectj.
  `org.springframework.transaction.event.TransactionalEventListener` needs it. Added `spring-tx`
  explicitly in 2.3 purely so the annotation compiles; U3.1's `spring-boot-starter-data-jpa` will
  bring it (and a real `PlatformTransactionManager`) transitively. Without a transaction manager a
  `@TransactionalEventListener` is silently skipped at publish time — fine for a "shown next to its
  siblings, exercised later" demo.
- CGLIB-proxied `@Service` beans: `@Component` / `@Service` / `@Repository` are **not `@Inherited`**, so
  `AnnotatedElementUtils.hasAnnotation(bean.getClass(), ...)` fails on the proxy subclass. Unwrap first
  with `ClassUtils.getUserClass(bean.getClass())` (or `AopUtils.getTargetClass`). Same reason a proxied
  bean's `getClass().getSimpleName()` is `Xxx$$SpringCGLIB$$0`.

## Overriding `<build><resources>`

- Overriding `<resources>` replaces Boot's default entirely. Always use the two-block form (one filtered
  resource limited to `application*.yml/yaml/properties`, one unfiltered resource excluding those) or non-config
  resources (prompts, templates, static) silently stop being copied.

## `@ConfigurationProperties` record: `@DefaultValue` rules

- A **bare** `@DefaultValue` (no value) on a scalar record component (`String`, `int`, ...) fails binding at
  startup: `IllegalStateException: Parameter of type java.lang.String must have a non-empty default value`.
- Bare `@DefaultValue` is only valid on a **nested value-object** parameter — it tells Boot to instantiate the
  nested record from its own per-field defaults even when the whole sub-prefix is absent.
- For a genuinely optional scalar, use `@Nullable` (JSpecify) and no `@DefaultValue`; for an optional scalar
  with a fallback, use `@DefaultValue("...")` with the literal.
- A `@NotNull @Valid Nested x` with no `@DefaultValue` binds to `null` when the sub-prefix is absent and then
  trips `@NotNull` — intended when the block is mandatory, a bug when you meant "optional with defaults".

## Banner `${application.version}` under `spring-boot:run`

- Boot's built-in `${application.version}` / `${application.formatted-version}` banner tokens read the jar
  manifest `Implementation-Version` — absent under `spring-boot:run`, so they render blank there. (build-info
  does NOT feed the banner; it feeds `/actuator/info`.)
- Fix: put `@project.version@` in `banner.txt` and add `**/banner.txt` to the **filtered** `<resources>` block (and to the unfiltered
  block's excludes) in the reactor parent. The `@...@` delimiter (set by
  spring-boot-starter-parent) leaves Spring's `${AnsiColor.*}` / `${spring-boot.formatted-version}` intact.
  Works for both `spring-boot:run` and the packaged jar.

## JSpecify `@NullMarked` does not cascade

- `@NullMarked` on a `package-info.java` covers that package only, not sub-packages. Add one
  `@NullMarked package-info.java` per package. `org.jspecify` is already on the compile classpath via
  Spring Framework 7, no explicit dependency needed.

## Boot 4: auto-configuration is split per-integration into its own module

- In Boot 4.x each integration's `*AutoConfiguration` lives in a dedicated
  `org.springframework.boot:spring-boot-<x>` module (`spring-boot-flyway`, `spring-boot-jpa`,
  `spring-boot-jdbc`, `spring-boot-liquibase`, ...). Adding the raw upstream library jar (`org.flywaydb:flyway-core`) puts the library on
  the classpath but **not** its Boot autoconfig —
  the app starts and silently does nothing (Flyway: no migration, empty schema, no
  `flyway_schema_history`). Depend on `spring-boot-flyway` (it pulls `flyway-core`) instead. When not
  using a `spring-boot-starter-*`, check whether a `spring-boot-<x>` autoconfig module exists in the BOM.
- Flyway 10+ ships no DB dialects in the core jar, so the target database's dialect module is a
  separate dependency: `org.flywaydb:flyway-database-oracle`.

## Testcontainers 2.0 renamed every module artifact

- Boot 4.1's BOM pins Testcontainers 2.0.x. In 2.0 every module artifactId gained a `testcontainers-`
  prefix: `org.testcontainers:oracle-free` → `testcontainers-oracle-free`,
  `junit-jupiter` → `testcontainers-junit-jupiter`, etc. The old names are **not** in the 2.0 BOM, so
  they fail with "'dependencies.dependency.version' ... is missing". The core jar is still
  `org.testcontainers:testcontainers`.
- Package layout mostly stable: the container class is `org.testcontainers.oracle.OracleContainer`
  (from the `oracle-free` module), `org.testcontainers.utility.DockerImageName` unchanged.

## `spring-boot:run` + a repo-root `compose.yaml` in a multi-module reactor

- (General fact; `spring-boot-training` does not use `spring-boot-docker-compose` — it assumes the
  database is already up.)
- `spring-boot-docker-compose` resolves `compose.yaml` against the **working directory**.
  `./mvnw -pl <module> spring-boot:run` forks in the module directory, so a repo-root `compose.yaml`
  is not found ("'files' content [compose.yaml] must exist"). Fix: set
  `<workingDirectory>${maven.multiModuleProjectDirectory}</workingDirectory>` on the
  `spring-boot-maven-plugin` in the module pom so the app runs from the reactor root. Prefer this over
  a brittle `spring.docker.compose.file: ../compose.yaml`.

## Once a datasource is on the classpath, every `@SpringBootTest` needs a database

- Adding `spring-boot-starter-data-jpa` means `DataSourceAutoConfiguration` runs for every full
  context; `@SpringBootTest` classes that previously needed no DB now fail at startup ("Failed to
  configure a DataSource"). Give them all a shared Testcontainers base class (`@SpringBootTest @ActiveProfiles("test")` + a singleton
  `@ServiceConnection` container as a `static`
  field started in a `static {}` block, no `@Testcontainers`). `ApplicationContextRunner` tests are
  unaffected — they never build a full context. `@ActiveProfiles` on a subclass merges with the base's (`inheritProfiles=true` by default).

## Flyway migrations are run manually via Maven, not on app boot

- User decision (U3.1 follow-up): `spring.flyway.enabled: false` in `application.yml`. Migrations are
  applied deliberately with the `flyway-maven-plugin` (in the module `<build><plugins>`, **no**
  `<executions>` so a normal build never triggers it):
  `./mvnw -pl spring-boot-training flyway:migrate` (also `flyway:info` / `flyway:validate` /
  `flyway:clean`), against a DB you started yourself (`docker compose up -d` — the app no longer
  starts one, see below).
- Plugin needs its own `<dependencies>` (plugin deps are not covered by the project BOM): the Oracle
  JDBC driver (`ojdbc17`) + `flyway-database-oracle`, versioned from the inherited BOM properties
  `${oracle-database.version}` / `${flyway.version}`. Connection config in `<configuration>` (`url` /
  `user` / `password`), overridable with `-Dflyway.url` etc.
- **Tests are the exception:** `application-test.yml` sets `spring.flyway.enabled: true`. The test
  datasource is a per-run Testcontainers container on a random port that `mvn flyway:migrate` can't
  reach, so Boot's `FlywayAutoConfiguration` migrates it on context start. `spring-boot-flyway`
  dependency is kept for exactly this.
- Once entities exist (3.2), booting the app against an un-migrated DB fails `ddl-auto: validate` -
  that is the intended "run `flyway:migrate` first" signal.

## App does not start its own database

- `spring-boot-docker-compose` is not used, to keep boot fast. The app assumes Oracle is already
  running. `application.yml` carries explicit `spring.datasource.url/username/password` (= the
  `compose.yaml` values, a throwaway local credential; prod overrides with `SPRING_DATASOURCE_URL` /
  `_USERNAME` / `_PASSWORD`). Start the DB with `docker compose up -d` before `spring-boot:run` or
  `flyway:migrate`.
- Tests are unaffected: Testcontainers `@ServiceConnection` registers a `JdbcConnectionDetails` bean
  that takes precedence over the `spring.datasource.*` properties.

## Spotless cache can hide a real formatting violation

- `spotless:check` skips files its cache thinks are clean. A wrapped multi-arg call that IntelliJ
  tab+space-aligned (`ij_java_align_multiline_parameters_in_calls=true`) can sit "clean" in the cache
  for months; the moment you edit that file the cache invalidates and `check` fails on the old
  alignment. `spotless:apply` fixes it to tabs-only — but the open IDE re-breaks it on the next
  filesystem-change notification, so run `spotless:apply` and then `verify` with nothing touching the
  files in between (chain them, don't let a tool call land between).

## The database is Oracle Database 23ai

- Local dev + Testcontainers use `gvenzl/oracle-free:23-slim` (Oracle Database 23ai Free). The app
  connects as the `training` app user to the `FREEPDB1` pluggable database:
  `jdbc:oracle:thin:@//localhost:1521/FREEPDB1`.
- Coordinates, all Spring Boot 4.1.1 BOM-managed: driver `com.oracle.database.jdbc:ojdbc17`
  (property `oracle-database.version`; `ojdbc17` is the JDK 17+ build — the right one for Java 25 +
  virtual threads); Flyway dialect `org.flywaydb:flyway-database-oracle` (`flyway.version`);
  Testcontainers module `org.testcontainers:testcontainers-oracle-free` (2.x name), container class
  `org.testcontainers.oracle.OracleContainer`. Boot's `spring-boot-testcontainers` ships an Oracle
  `@ServiceConnection` factory.

## Oracle DDL / SQL notes

- Types: `number(19)` for a 64-bit id, `varchar2(n)`, `number(p,s)`, `timestamp with time zone`;
  `systimestamp` for the current timestamp default.
- Column clause order: `default <expr>` comes **before** `not null`
  (`... default systimestamp not null`).
- `generated always as identity` and `constraint ... check/unique/foreign key` are standard.
- Unquoted identifiers are folded to **UPPER case** in the data dictionary. Query `user_tables` /
  `user_constraints` (there is no `information_schema`); FK constraints have `constraint_type = 'R'`.
- Flyway creates its schema-history table with **quoted, lower-case** identifiers on Oracle, so from
  SQL you must quote them: `select "success" from "flyway_schema_history" where "version" = '1'`
  (unquoted `FLYWAY_SCHEMA_HISTORY` → ORA-00942). `"success"` is `NUMBER(1)` — read as int.

## Sample data: two-tier, Flyway-owned, repeatable

- User decision (U3.1 follow-up): seed data is **Flyway**, not `data.sql` / an `ApplicationRunner`
  seeder. Two tiers:
	- **Baseline** `db/migration/R__0*_seed_<feature>.sql` - small, idempotent, keyed on natural
	  keys with `MERGE`. In `db/migration`, so the `test` profile's Boot Flyway applies it after the
	  DDL and every `@SpringBootTest` starts from it. Pair one with each `V*` entity DDL in the same
	  commit.
	- **Demo bulk** `db/demo/R__9*_demo_<feature>.sql` - large, generated with Oracle
	  `CONNECT BY LEVEL <= N` + `DBMS_RANDOM`. `db/demo` is a `<location>` on the flyway-maven-plugin (module pom), so
	  `./mvnw flyway:migrate` applies it locally; the Testcontainers tests never see
	  it because Boot reads `spring.flyway.locations: classpath:db/migration`. **Not** a Maven-profile
	  toggle - see the gotcha below.
- **Repeatable (`R__`), not versioned.** They re-run on checksum change, so the seed evolves with
  the entities without version-number juggling or `flyway:clean`. Numeric prefix in the
  description (`R__010_…`, `R__020_…`) controls their order (they run after all `V*`, alphabetically
  by description).
- **Idempotency patterns.** Baseline: `MERGE INTO t USING (SELECT … FROM dual UNION ALL …) src ON
  (t.<natkey> = src.<natkey>) WHEN MATCHED THEN UPDATE … WHEN NOT MATCHED THEN INSERT …`. Bulk: own
  a marker (`sku LIKE 'GEN-%'`), `DELETE` it, re-`INSERT`. Seed `DBMS_RANDOM` in a
  `begin dbms_random.seed(n); end;` PL/SQL block terminated by `/` on its own line (Flyway's Oracle
  parser needs the `/`) for reproducible values.
- **Don't assume contiguous identity ids** when mapping generated rows to parent rows - use
  `row_number() over (order by id)` over the parent table, not `min(id) + mod(...)`.
- **flyway-maven-plugin locations.** List every dir as a `<location>` under the plugin
  `<configuration>/<locations>` (each `filesystem:${project.basedir}/…` on its own line, under the
  140-col limit). A single `<flyway.locations>` CSV *property* also works but a long one-line value
  trips the col limit, and IDEA then wraps it across lines - which the plugin rejects with
  `No value provided for placeholder: ${flyway.locations}`. Override for a one-off with
  `-Dflyway.locations=…` on the command line.
- **Gotcha: never gate a *tracked* repeatable behind a conditional location.** The demo bulk was
  first shipped as `R__910` loaded only via a `-Pdemo` Maven profile. After one `-Pdemo` run, a
  plain `flyway:migrate` aborts: `FlywayValidateException: Detected applied migration not resolved
  locally: 910 demo catalog` - Flyway 12's `migrate` runs `validate` first and a tracked repeatable
  missing from the path is a **hard error**, not a warning. Fix: `db/demo` is **always** in the
  plugin's `<locations>`; the split that matters (local vs test) is handled by Boot's separate
  `spring.flyway.locations: classpath:db/migration`. If a dataset genuinely must be conditional,
  make it a callback (`afterMigrate__*.sql`), which is not written to `flyway_schema_history`.
- Tests that assert on **exact** rows still use `@Sql` with their own fixture and check their own
  marked rows - never global counts against the shared baseline.
- **Never edit an applied `V*` migration**, not even a comment - it changes the checksum and
  `flyway:migrate` then fails validation on any DB that already ran it ("Migration checksum
  mismatch for migration version N"). The compose Oracle keeps its schema across sessions (named
  `oracle-data` volume), so this bites locally even when tests (fresh Testcontainers) stay green.
  Put pairing notes in the `R__` file / docs, not in the `V*`.
- `flyway:clean` is **disabled by default** in Flyway 10+ (`flyway.cleanDisabled=true`). Run it as
  `./mvnw -pl spring-boot-training -Dflyway.cleanDisabled=false flyway:clean flyway:migrate`.

## `gvenzl/oracle-free` compose best practice

- `shm_size: '1g'` is not optional — Oracle dies at startup on Docker's 64 MB `/dev/shm` default (ORA-27101 / "MEMORY_TARGET not
  supported").
- Healthcheck: the image ships `healthcheck.sh` (`test: ['CMD', 'healthcheck.sh']`). First-ever boot
  builds the database and takes minutes — set a generous `start_period`.
- `APP_USER` **and** `APP_USER_PASSWORD` are both required to auto-create the app user (in `FREEPDB1`).
  `ORACLE_PASSWORD` is SYS/SYSTEM.
- Datafiles volume: `/opt/oracle/oradata`. First-boot-only init scripts: `/container-entrypoint-initdb.d`
  (`*.sql` / `*.sql.gz` / `*.sh`); every-startup scripts: `/container-entrypoint-startdb.d`.
- Image is large (~2 GB) and slow to first-start; cache the container once per JVM via the singleton
  pattern so `verify` starts Oracle only once.
- On a small host (8 GB), stacking two `verify` runs (compose Oracle + the previous run's Testcontainers
  Oracle not yet reaped + a new one) can make the new container miss its `DATABASE IS READY TO USE!`
  log wait → `ContainerLaunchException: Timed out ...` → `AbstractOracleIntegrationTest.<clinit>` throws →
  every dependent IT fails with `NoClassDefFoundError`. Transient; re-run. Not a code fault.

## Spring Data JPA auditing does not support `OffsetDateTime` (U3.2)

- `@CreatedDate` / `@LastModifiedDate` fields typed `OffsetDateTime` (or `ZonedDateTime`) blow up at **persist time**, not startup: `IllegalArgumentException: Cannot convert unsupported date type
  java.time.LocalDateTime to java.time.OffsetDateTime` from `DefaultAuditableBeanWrapperFactory`. The
  supported set is `LocalDateTime`, `LocalDate`, `LocalTime`, `Instant`, `java.util.Date`, `Long`, `long`.
- Fix: type the audit fields `Instant`. The catalog columns are Oracle `timestamp with time zone` (from
  `V1`), so add `@JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)` to keep Hibernate mapping the `Instant`
  onto that column type (validate then stays green). Plain non-audited temporal fields (`Order.placedAt`)
  can stay `OffsetDateTime` — Hibernate itself maps that to `timestamp with time zone` fine; it is only
  the Spring Data auditing converter that is fussy.
- `@EnableJpaAuditing` must be present somewhere (`config/persistence/PersistenceConfig`) or the
  `AuditingEntityListener` on the `@MappedSuperclass` silently never runs.

## Reserved-word table name (`order`) on Oracle + Hibernate + Flyway (U3.2)

- Annotate the entity `@Table(name = "`order`")` — backticks are Hibernate's portable "quote this
  identifier" marker; it emits `"order"` on Oracle. Flyway DDL/seed scripts must `create table "order"` /
  `from "order"` (quoted) too, and any hand-written JDBC/JdbcClient SQL as well.
- Oracle folds unquoted identifiers to UPPER case in the data dictionary, but a quoted lower-case
  `"order"` stays lower-case — so it is `"order"` everywhere in SQL, never `ORDER` or `"ORDER"`.

## Hibernate 7 record `@Embeddable`

- A Java `record` works as an `@Embeddable` for a plain `@Embedded` field (immutable value object).
  `@Column` / `@AttributeOverride` on the record components map and validate normally. Keeps the
  "records for value objects, Lombok only for entities" rule intact.

## Spring Data JDBC alongside Spring Data JPA — coexist, but scope each scanner (U3.5)

- Adding `spring-boot-starter-data-jdbc` next to `spring-boot-starter-data-jpa` works: Spring Data's **strict multi-store detection**
  (auto-on with 2+ Spring Data modules) routes each repository by
  its aggregate — `@org.springframework.data.relational.core.mapping.Table` +
  `ListCrudRepository`/`CrudRepository` → JDBC; `@jakarta.persistence.Entity` + `JpaRepository` → JPA.
- **But** each scanner still examines *every* repository interface and logs an INFO line
  `Could not safely identify store assignment for repository candidate interface …` for the ones it
  can't claim (verify stays green — it's noise, not a failure, and only visible on `spring-boot:run`,
  not in the test log). Fix: pin each module to its own package —
  `@EnableJpaRepositories(basePackages = "…catalog")` and
  `@EnableJdbcRepositories(basePackages = "…reporting")` in `config/persistence` (`PersistenceConfig`
	+ a new `JdbcConfig`). `@EnableJpaRepositories` replaces Boot's `JpaRepositoriesAutoConfiguration`;
	  `@EnableJdbcRepositories` only registers the repo config extension, so Boot still supplies
	  `JdbcAggregateTemplate` / dialect / converters.
- The reporting aggregate must import the `org.springframework.data.*` annotations only (`org.springframework.data.annotation.Id`, `org.springframework.data.relational.core.mapping.{Table,
  Column,MappedCollection}`). A stray `jakarta.persistence.*` import misroutes the repo (or fails
  mapping). Records work as Spring Data JDBC aggregate roots; a `Set<Child>` with no `@Id` on the
  child is the "owned collection" case — Spring Data deletes-all-by-FK + re-inserts on every save.
- `spring.data.jdbc.dialect: oracle` is optional (auto-resolves `JdbcOracleDialect` from ojdbc17
  metadata) but explicit is cheaper and clearer.

## `JdbcClient` has no batch API in Spring Framework 7.x (U3.5)

- `JdbcClient` (`org.springframework.jdbc.core.simple`) has no batch method — `params(List)` is
  positional params for **one** statement, `update()` returns one `int`. A real batch drops to
  `NamedParameterJdbcTemplate.batchUpdate(sql, SqlParameterSource[])`.
- Build the `SqlParameterSource[]` with `SimplePropertySqlParameterSource` (spring-jdbc 7.x,
  reads record accessors / property paths). **Not** `SqlParameterSourceUtils.createBatch(list)` —
  it wraps each element in `BeanPropertySqlParameterSource`, which reads JavaBean getters only and
  binds **nothing** from a record.
- Skipping generated-key retrieval (caller supplies the natural key) also sidesteps Oracle's
  "identity column + JDBC batch" limitation.

## Spring Data JDBC quotes `@Table` / `idColumn` literals — must be UPPER case on Oracle (U3.5)

- Spring Data JDBC renders a `@Table("name")` / `@MappedCollection(idColumn = "name")` /
  `@Column("name")` **string literal** as a *quoted* identifier, verbatim. Oracle folds unquoted
  DDL identifiers to upper case, so a quoted lower-case `"report_snapshot"` does not resolve (`ORA-00942` / `ORA-00904`). Write the literals
  upper case: `@Table("REPORT_SNAPSHOT")`,
  `idColumn = "SNAPSHOT_ID"`.
- **Derived** names (record component / class name → naming strategy → column/table) are produced *unquoted* and fold to upper case on their
  own — no annotation needed, no casing issue. So the
  rule is only about explicit string literals.
- `JdbcClient` / `NamedParameterJdbcTemplate` SQL is passed through literally (not quoted), so
  lower-case `sales_ledger` in a `.sql("select … from sales_ledger …")` is fine — same as the
  Flyway scripts and the catalog `@Table(name = "category")` under Hibernate. The quoting behaviour
  is specific to Spring Data JDBC's identifier handling.