# Task: Create the parent pom (+ Maven wrapper)

Plan: `~/.claude/plans/silly-leaping-micali.md`

## Items

- [x] Create root `../../pom.xml` (parent, `packaging = pom`) per the `maven-scaffold` parent skeleton
	- [x] Coordinates `gr.codelearn.springbootai:spring-boot-ai:0.0.1-SNAPSHOT`
	- [x] Maven parent `spring-boot-starter-parent:4.1.1`
	- [x] Properties: `maven.version=3.9.16`, `java.version=25`, `spring-ai.version=2.0.1`, mapstruct + binding, jacoco, spotless
	- [x] `<dependencyManagement>`: import `spring-ai-bom`
	- [x] Build: enforcer, compiler (Java 25 + annotation processors), spotless, jacoco; resources; defaultGoal/directory/finalName
	- [x] `<scm>` + `<distributionManagement>` under `codehub-learn/claude-code-spring-boot-ai`
	- [x] tabs, CRLF, UTF-8, no final newline
- [x] Generate Maven wrapper pinned to 3.9.16 (`mvnw`, `mvnw.cmd`, `../../.mvn/wrapper/maven-wrapper.properties`)
- [x] Verify: `mvnw -v`, `mvnw validate`, `mvnw help:effective-pom`, `mvnw spotless:check`
- [x] Confirm `git status` shows only the intended new files

## Review

Done. New files: `../../pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, ``,
``.

- `./mvnw -v` -> Apache Maven 3.9.16, Java 25.
- `./mvnw validate` -> BUILD SUCCESS, no warnings.
- `./mvnw help:effective-pom` -> `spring-ai-bom` import expands (168 managed `org.springframework.ai` entries).
- `./mvnw spotless:check` -> BUILD SUCCESS (no Java sources yet).
- `../../pom.xml` is tabs / CRLF / UTF-8 / no final newline; `git diff --check` clean.

Adjustments made during implementation:

- Added `jacoco-maven-plugin.version` (0.8.13) + explicit `<version>` on the plugin: `spring-boot-starter-parent`
  4.1.1 does **not** manage jacoco, so the version was missing.
- `<resources>` uses the two-block form (filtered `application*` + unfiltered rest) instead of the skeleton's
  single filtered block, which would have dropped every other resource (prompts, templates, static).

Known gap (deferred, per plan): the spotless `<java>` config falls back to google-java-format (2-space), which
conflicts with the `../../.editorconfig` tab rule. Harmless now (no Java); needs a tab-aware formatter config before
the first module lands.