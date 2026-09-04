# Changelog

## 1.1.1 - 2026-09-04 11:08

- Added a rule that web modules use `spring-boot-starter-webmvc` (the Boot 4 name), never the deprecated `spring-boot-starter-web` alias.

## 1.1.0 - 2026-09-04 11:08

- Added the three pom shapes (parent, aggregator, code module) with their exact section order and the rules every pom must follow (baseline
  entry; predates the changelog convention).

## 1.0.0 - 2026-09-03 13:25

- Added the initial `maven-scaffold` skill: three pom skeletons (parent, aggregator, code module), their section order, and the rules every
  pom must follow, extracted from `CLAUDE.md`.