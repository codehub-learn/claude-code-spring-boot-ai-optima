# Changelog

## 1.1.0 - 2026-09-03 19:45

- Clarified the groupId rule: the reactor `groupId` is always the flat `gr.codelearn`, never `gr.codelearn.<product>`. The product name
  lives in the `artifactId` and the Java package. Fixed the parent skeleton's `<groupId>` placeholder accordingly.

## 1.0.0 - 2026-09-03 13:25

- Added the initial `maven-scaffold` skill: three pom skeletons (parent, aggregator, code module), their section order, and the rules every
  pom must follow, extracted from `CLAUDE.md`.