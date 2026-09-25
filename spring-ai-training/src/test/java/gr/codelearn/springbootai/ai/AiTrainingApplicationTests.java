package gr.codelearn.springbootai.ai;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.Test;

// "test" profile: quiet logging and a dummy Anthropic API key, so the test never uses a developer's real ANTHROPIC_API_KEY.
class AiTrainingApplicationTests extends AbstractOracleIntegrationTest {

	@Test
	void contextLoads() {
	}
}