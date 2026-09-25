package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 7.1: the chat provider is picked by configuration, not code. Starts the real application (all auto-configuration, the real
 * {@code config.ai.ChatClientConfig}) with both provider starters on the classpath and varies only {@code spring.ai.model.chat}. Nothing
 * here calls a model: neither an API key nor a running Ollama server is needed, since both clients connect lazily.
 */
class ChatModelSelectionTest {

	@Test
	void theDefaultConfigurationSelectsTheCloudModel() {
		try (ConfigurableApplicationContext context = start()) {
			assertThat(context.getBeansOfType(ChatModel.class)).hasSize(1)
															   .allSatisfy((name, model) -> assertThat(model).isInstanceOf(
																	   AnthropicChatModel.class));
			assertThat(context.getBean(ChatClient.class)).isNotNull();
		}
	}

	@Test
	void onePropertySwitchesToTheLocalModelWithoutTouchingFeatureCode() {
		try (ConfigurableApplicationContext context = start("--spring.ai.model.chat=ollama")) {
			assertThat(context.getBeansOfType(ChatModel.class)).hasSize(1)
															   .allSatisfy((name, model) -> assertThat(model).isInstanceOf(
																	   OllamaChatModel.class));
			assertThat(context.getBean(ChatClient.class)).isNotNull();
		}
	}

	@Test
	void theOllamaEmbeddingModelStaysOffUntilRag() {
		try (ConfigurableApplicationContext context = start("--spring.ai.model.chat=ollama")) {
			assertThat(context.getBeansOfType(EmbeddingModel.class)).isEmpty();
		}
	}

	@Test
	void theLocalModelIsTheSmallInstructModelPickedIn72NotSpringAisFallback() {
		// Unit 7.2: application.yml names the local model. Unset, OllamaChatOptions would fall back to mistral (7B).
		try (ConfigurableApplicationContext context = start("--spring.ai.model.chat=ollama")) {
			OllamaChatOptions options = context.getBean(OllamaChatModel.class).getOptions();
			assertThat(options.getModel()).isEqualTo("qwen3.5:4b");
			assertThat(options.getThinkOption()).isEqualTo(ThinkOption.ThinkBoolean.DISABLED);
			assertThat(options.getNumCtx()).isEqualTo(8192);
		}
	}

	@Test
	void leavingThePropertyUnsetActivatesBothProvidersAndTheChatClientCannotBeBuilt() {
		// Each provider's chat autoconfig has matchIfMissing = true: with two starters and no choice, both ChatModel beans are created and
		// the ChatClient.Builder (one ChatModel parameter) can't pick. The app fails at startup, in ChatClientConfig.
		// spring.config.name=none skips application*.yml entirely, so the property is really absent (not just empty).
		assertThatThrownBy(() -> start("--spring.config.name=none", "--spring.ai.anthropic.api-key=test-key-not-used").close())
				.hasStackTraceContaining("expected single matching bean but found 2");
	}

	@Test
	void aValueNamingNoStarterOnTheClasspathLeavesNoChatModelAtAll() {
		// A typo, or a provider whose starter is missing: no autoconfig matches, and the failure again surfaces in ChatClientConfig.
		assertThatThrownBy(() -> start("--spring.ai.model.chat=openai").close())
				.hasStackTraceContaining("ChatModel")
				.hasStackTraceContaining("NoSuchBeanDefinitionException");
	}

	private static ConfigurableApplicationContext start(final String... args) {
		// "test" profile: dummy Anthropic key, quiet logs. No web server: the selection happens in the bean graph, not over HTTP.
		return new SpringApplicationBuilder(AiTrainingApplication.class).web(WebApplicationType.NONE)
																		.profiles("test")
																		.run(args);
	}
}