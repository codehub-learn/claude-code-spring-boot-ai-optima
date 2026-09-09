package gr.codelearn.springbootai.training.foundation.components;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: {@code @Value}, {@code Environment}, and the typed {@code @ConfigurationProperties} record all read the same {@code app.name}
 * and agree — here, the {@code application-test.yml} override.
 */
class ConfigAccessDemoTest extends AbstractOracleIntegrationTest {

	@Autowired
	private ConfigAccessDemo demo;

	@Test
	void allThreeAccessStylesResolveTheSameValue() {
		assertThat(demo.viaValueAnnotation()).isEqualTo("Training (test)");
		assertThat(demo.viaEnvironment()).isEqualTo("Training (test)");
		assertThat(demo.viaConfigurationProperties()).isEqualTo("Training (test)");
	}
}