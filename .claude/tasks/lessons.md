# Lessons

Patterns captured after user corrections. Review at session start.

## todo.md: never edit the checklist item text

- In `.claude/tasks/todo.md`, the `- [ ]` items are the plan. Only ever flip `[ ]` -> `[x]` when a topic
  is complete. Do **not** append "Done: ..." / "Deviations: ..." to the item body — that goes in the
  `## Review` section as a `### <topic> (done)` block. User correction, 2026-09-04.
- Never add a new checklist step on your own. The user adds steps (e.g. "1.4") explicitly; when asked,
  ask the clarifying questions needed to pin down its scope before writing it into the plan.

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

## IntelliJ auto-format diverges from Spotless

- The open IDE reformats `.java` files on open/save; its style differs from the Spotless config, which
  caused `spotless:check` failures repeatedly.
- **Import order (fixed 2026-09-04).** Bare `<importOrder/>` (static first, one flat group) fought
  `.editorconfig`'s `ij_java_imports_layout = *, |, javax.**, java.**, |, $*`. The parent pom now pins
  `<importOrder><order>,javax|java,\#</order></importOrder>` to match `.editorconfig`, so `spotless:apply`
  and the IDE produce the same import block. `spotless:check` only fails, never edits — the two editors
  are `spotless:apply` and IntelliJ.
- **Still diverges: multiline arg alignment.** IJ (`ij_java_align_multiline_parameters_in_calls = true`)
  aligns wrapped call args with `tab+space`; Spotless `<indent><tabs>` wants tabs only. Not yet
  reconciled.
- Always run `./mvnw spotless:apply` as the **last** step before `verify`, after any IDE has touched the
  files. Do not trust a green build from before the file was opened in IntelliJ.

## Boot 4 web starter name

- Use `spring-boot-starter-webmvc`, not `spring-boot-starter-web` (the 4.1 POM marks the latter
  deprecated). Recorded in CLAUDE.md tech-stack table and `maven-scaffold` v1.1.1.

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