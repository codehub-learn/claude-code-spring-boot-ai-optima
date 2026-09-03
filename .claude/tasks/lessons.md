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

## groupId is flat `gr.codelearn`

- The reactor `groupId` is always the flat `gr.codelearn`, never `gr.codelearn.<product>` or
  `gr.codelearn.<app>`. The product name lives in the `artifactId` and the Java package (`gr.codelearn.<product>.<app>`), not the `groupId`.
  I first wrote `gr.codelearn.springbootai`
  as the groupId and CLAUDE.md said `com.giannacoulis.<app>` for the root package; both were
  wrong. Fixed in `pom.xml`, `spring-boot-training/pom.xml`, `CLAUDE.md`, and the `maven-scaffold`
  skill (v1.1.0).

## Overriding `<build><resources>`

- Overriding `<resources>` replaces Boot's default entirely. Always use the two-block form (one filtered
  resource limited to `application*.yml/yaml/properties`, one unfiltered resource excluding those) or non-config
  resources (prompts, templates, static) silently stop being copied.