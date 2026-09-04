package gr.codelearn.springbootai.training;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

/**
 * Verifies the two Actuator endpoints wired in topic 1.1: {@code health} aggregates to {@code UP} and {@code info} carries the static
 * {@code app} metadata from {@code application.yml}. Asserting against the endpoint beans keeps this a context test (no running server, no
 * HTTP client).
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
}