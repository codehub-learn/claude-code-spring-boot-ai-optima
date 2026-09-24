package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// "test" profile: quiet logging and a dummy Anthropic API key, so the test never uses a developer's real ANTHROPIC_API_KEY.
@SpringBootTest
@ActiveProfiles("test")
class AiTrainingApplicationTests {

	@Test
	void contextLoads() {
	}
}
