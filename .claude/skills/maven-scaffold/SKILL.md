---
name: maven-scaffold
description: |
	Use when creating or modifying Maven pom files in this reactor: "create the parent pom", "add an aggregator / module group", "add a module / product / app", or any pom skeleton / section-order question. Provides the three pom shapes (parent, aggregator, code module), their exact section order, and the rules every pom must follow.
version: 1.1.0
author: Constantinos Giannacoulis
---

This repo is a Maven reactor that co-hosts multiple codebases (products) under one Git repo. Modules may depend on each other; the driver is
co-hosting. Three pom shapes exist. When asked to "create the parent pom", "add an aggregator / module group", or "add a module / product /
app", generate the matching skeleton below.

## Rules that apply to every pom

- Tabs for indentation, CRLF, UTF-8, no final newline (`.editorconfig`); Spotless enforces.
- `<project>` attribute order: `xmlns:xsi`, then `xmlns`, then `xsi:schemaLocation`.
- Keep the `<!-- ... -->` banner comments and the single blank line between sections exactly as the skeletons show.
- `<name>` is always `[${project.artifactId}]`.
- Reactor `groupId` is always the flat `gr.codelearn` (never `gr.codelearn.<product>`); the product name lives in the `artifactId` and the
  Java package, not the `groupId`. Child modules inherit `groupId` and `version` and declare neither.
- `<organization>` is always `Code.Learn by Code.Hub` / `https://www.codehub.gr/codelearn/`; SCM and distribution URLs sit under
  `github.com/codehub-learn/<repo>`.
- The skeletons define **structure only**. Never copy libraries or versions out of them or out of any template; pick dependencies and
  versions from the tech-stack table in `CLAUDE.md` and give each entry a short `<!-- ... -->` comment, grouping related entries with a
  blank
  line between groups.
- Native Boot structured logging is the choice (see *Observability* in `CLAUDE.md`): do **not** exclude `spring-boot-starter-logging`, do
  **not** add Log4j2 / Disruptor, do **not** add a log4j version-ban enforcer rule.

## Parent pom (`packaging = pom`, at the repo root)

Section order: Packaging, Versioning (coordinates + `spring-boot-starter-parent` as Maven parent), Meta-data (with `<organization>` and
`<inceptionYear>`), `<modules>`, Properties, Dependency Management, Dependencies, Build settings (`<pluginManagement>`, then `<plugins>`,
then
`<resources>`, then `defaultGoal` / `directory` / `finalName`), `<pluginRepositories>`, Profiles, `<scm>`, Distribution Management. Drop
`<pluginRepositories>` and `<profiles>` entirely when nothing needs them.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xmlns="http://maven.apache.org/POM/4.0.0"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<!-- Packaging -->
	<modelVersion>4.0.0</modelVersion>
	<packaging>pom</packaging>

	<!-- Versioning -->
	<groupId>gr.codelearn</groupId>
	<artifactId>...</artifactId>
	<version>...</version>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>...</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>

	<!-- Meta-data -->
	<name>[${project.artifactId}]</name>
	<description>...</description>
	<organization>
		<name>Code.Learn by Code.Hub</name>
		<url>https://www.codehub.gr/codelearn/</url>
	</organization>
	<inceptionYear>2026</inceptionYear>

	<modules>
		...
	</modules>

	<!-- Properties/Variables -->
	<properties>
		<!-- Desired Maven version -->
		<maven.version>...</maven.version>
		<!-- Build JDK -->
		<java.version>25</java.version>

		<!-- Maven source encoding -->
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

		<!-- <library group> -->
		<...version>...
	</...version>

			<!-- Maven plugins -->
	<...-plugin.version>...
</...-plugin.version>
		</properties>

		<!-- Dependency Management -->
<dependencyManagement>
<dependencies>
	...
</dependencies>
</dependencyManagement>

		<!-- Dependencies -->
<dependencies>
...
</dependencies>

		<!-- Build settings -->
<build>
<!-- Plugin Management -->
<pluginManagement>
	<plugins>
		<!-- maven-enforcer-plugin: enforce-versions execution asserting requireJavaVersion + requireMavenVersion -->
		<!-- maven-compiler-plugin: <release>${java.version}</release>, <fork>true</fork>, annotationProcessorPaths for
			 spring-boot-configuration-processor, Lombok, MapStruct, lombok-mapstruct-binding -->
		...
	</plugins>
</pluginManagement>

<!-- Plugins -->
<plugins>
	...
</plugins>

<!-- Resources -->
<resources>
	<resource>
		<directory>src/main/resources</directory>
		<includes>
			...
		</includes>
		<filtering>true</filtering>
	</resource>
</resources>

<defaultGoal>package</defaultGoal>
<directory>${basedir}/target</directory>
<finalName>${project.artifactId}-${project.version}</finalName>
</build>

<pluginRepositories>
...
</pluginRepositories>

		<!-- Profiles -->
<profiles>
...
</profiles>

<scm>
<connection>scm:git:https://github.com/codehub-learn/...</connection>
<developerConnection>scm:git:https://github.com/codehub-learn/...</developerConnection>
<url>https://github.com/codehub-learn/...</url>
</scm>

		<!-- Distribution Management -->
<distributionManagement>
<repository>
	<id>github</id>
	<url>https://maven.pkg.github.com/codehub-learn/...</url>
</repository>
</distributionManagement>
		</project>
```

## Aggregator module (`packaging = pom`, groups sibling modules)

Section order: Packaging, Versioning (`artifactId` + `<parent>`), Meta-data, `<modules>`. Nothing else unless a real need appears.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xmlns="http://maven.apache.org/POM/4.0.0"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<!-- Packaging -->
	<modelVersion>4.0.0</modelVersion>
	<packaging>pom</packaging>

	<!-- Versioning -->
	<artifactId>...</artifactId>
	<parent>
		<groupId>...</groupId>
		<artifactId>...</artifactId>
		<version>...</version>
	</parent>

	<!-- Meta-data -->
	<name>[${project.artifactId}]</name>
	<description>...</description>

	<modules>
		...
	</modules>
</project>
```

## Code module (`packaging = jar`, implicit)

Section order: Packaging (`modelVersion` only), Versioning (`artifactId` + `<parent>`), Meta-data, Properties, Dependencies, Build. The
`<build>` block carries `spring-boot-maven-plugin` **only for a runnable application module** (`<excludeGroupIds>` for non-runtime
processors such as `org.mapstruct` / `org.projectlombok`, `<layers><enabled>true</enabled></layers>`, `<mainClass>`, and a `process-aot`
execution). Library modules omit `<build>` entirely.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xmlns="http://maven.apache.org/POM/4.0.0"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<!-- Packaging -->
	<modelVersion>4.0.0</modelVersion>

	<!-- Versioning -->
	<artifactId>...</artifactId>
	<parent>
		<groupId>...</groupId>
		<artifactId>...</artifactId>
		<version>...</version>
	</parent>

	<!-- Meta-data -->
	<name>[${project.artifactId}]</name>
	<description>...</description>

	<!-- Properties/Variables -->
	<properties>
		...
	</properties>

	<!-- Dependencies -->
	<dependencies>
		...
	</dependencies>

	<build>
		<!-- Plugins -->
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludeGroupIds>
						...
					</excludeGroupIds>
					<layers>
						<enabled>true</enabled>
					</layers>
					<mainClass>...</mainClass>
				</configuration>
				<executions>
					<execution>
						<id>process-aot</id>
						<goals>
							<goal>process-aot</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
```