# Lessons

Patterns captured after user corrections. Review at session start.

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

- The open IDE reformats `.java` files on open/save, and its style differs from the Spotless config (import grouping: IJ keeps `java.**` in
  its own group per `.editorconfig`, Spotless' bare
  `<importOrder/>` wants one flat alphabetical group; IJ aligns call args with `tab+space`,
  Spotless wants tabs only). This has caused `spotless:check` failures twice.
- Always run `./mvnw -pl <module> spotless:apply` as the **last** step before `verify`, after any
  IDE has touched the files. Do not trust a green build from before the file was opened in IntelliJ.

## Boot 4 web starter name

- Use `spring-boot-starter-webmvc`, not `spring-boot-starter-web` (the 4.1 POM marks the latter
  deprecated). Recorded in CLAUDE.md tech-stack table and `maven-scaffold` v1.1.1.

## Overriding `<build><resources>`

- Overriding `<resources>` replaces Boot's default entirely. Always use the two-block form (one filtered
  resource limited to `application*.yml/yaml/properties`, one unfiltered resource excluding those) or non-config
  resources (prompts, templates, static) silently stop being copied.