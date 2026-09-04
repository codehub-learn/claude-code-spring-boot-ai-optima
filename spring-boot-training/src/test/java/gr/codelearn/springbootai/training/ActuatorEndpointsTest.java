package gr.codelearn.springbootai.training;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

/**
 * Verifies the Actuator endpoints wired in topics 1.1 and 1.2: {@code health} aggregates to {@code UP}, and {@code info} carries the static
 * {@code app} metadata from {@code application.yml} plus the {@code build} and {@code git} groups contributed from the files the Maven
 * build produces ({@code build-info.properties}, {@code git.properties}). Asserting against the endpoint beans keeps this a context test
 * (no running server, no HTTP client). The {@code build}/{@code git} assertions assume a full {@code ./mvnw} build ran first (both goals
 * fire before the {@code test} phase); running only this class from an IDE without that step will not have generated the files.
 */
@SpringBootTest
class ActuatorEndpointsTest {
	@Autowired
	private HealthEndpoint healthEndpoint;

	@Autowired
	private InfoEndpoint infoEndpoint;

	@Test
	void healthEndpointAggregatesToUp() {
		assertThat(healthEndpoint.health().getStatus().getCode()).isEqualTo("UP");
	}

	@Test
	void infoEndpointExposesAppMetadata() {
		assertThat(infoEndpoint.info())
				.extractingByKey("app", map(String.class, Object.class))
				.containsEntry("name", "spring-boot-training");
	}

	@Test
	void infoEndpointExposesBuildAndGitGroups() {
		assertThat(infoEndpoint.info())
				.extractingByKey("build", map(String.class, Object.class))
				.containsEntry("artifact", "spring-boot-training")
				.containsKey("time");

		assertThat(infoEndpoint.info())
				.extractingByKey("git", map(String.class, Object.class))
				.containsKey("branch")
				.hasEntrySatisfying("commit", commit -> assertThat(commit).isInstanceOf(java.util.Map.class));
	}
}