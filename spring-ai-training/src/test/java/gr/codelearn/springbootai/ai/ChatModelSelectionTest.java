package gr.codelearn.springbootai.ai;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Units 7.1 and 7.4: the chat provider is picked by configuration, not code. Starts the real application (all auto-configuration, the real
 * {@code config.ai.ChatClientConfig}) with both provider starters on the classpath and varies only the configuration: the
 * {@code app.ai.chat-provider} switch, directly or through a profile. Nothing here calls a model except the retry test, which calls an
 * Ollama address where nothing listens: neither an API key nor a running Ollama server is needed, since both clients connect lazily.
 */
class ChatModelSelectionTest {

	@Test
	void theDefaultConfigurationSelectsTheCloudModel() {
		try (ConfigurableApplicationContext context = start()) {
			assertSingleChatModel(context, AnthropicChatModel.class);
			assertThat(context.getBean(ChatClient.class)).isNotNull();
		}
	}

	@Test
	void onePropertySwitchesToTheLocalModelWithoutTouchingFeatureCode() {
		try (ConfigurableApplicationContext context = start("--app.ai.chat-provider=ollama")) {
			assertSingleChatModel(context, OllamaChatModel.class);
			assertThat(context.getBean(ChatClient.class)).isNotNull();
		}
	}

	@Test
	void theLocalProfileRunsOnTheOfflineModel() {
		try (ConfigurableApplicationContext context = startWithProfiles("local")) {
			assertSingleChatModel(context, OllamaChatModel.class);
		}
	}

	@Test
	void theAnthropicProfileRunsOnTheCloudModelEvenOnTopOfLocal() {
		// Profiles are applied in order and the last one wins: local's ollama is overridden by anthropic's anthropic.
		try (ConfigurableApplicationContext context = startWithProfiles("local", "anthropic")) {
			assertSingleChatModel(context, AnthropicChatModel.class);
		}
	}

	@Test
	void embeddingsComeFromOllamaWhicheverChatProviderIsActive() {
		// Unit 10.1 (was "no EmbeddingModel until RAG" in 7.1): Anthropic has no embeddings API, so RAG embeds with Ollama under both
		// chat providers. The chat switch and the embedding switch are independent.
		try (ConfigurableApplicationContext context = start()) {
			assertSingleChatModel(context, AnthropicChatModel.class);
			assertThat(context.getBeansOfType(EmbeddingModel.class).values()).singleElement().isInstanceOf(OllamaEmbeddingModel.class);
			assertThat(context.getEnvironment().getProperty("spring.ai.ollama.embedding.model")).isEqualTo("qwen3-embedding:0.6b");
		}
	}

	@Test
	void theLocalModelIsTheSmallInstructModelPickedIn72NotSpringAisFallback() {
		// Unit 7.2: application.yml names the local model. Unset, OllamaChatOptions would fall back to mistral (7B).
		try (ConfigurableApplicationContext context = start("--app.ai.chat-provider=ollama")) {
			OllamaChatOptions options = context.getBean(OllamaChatModel.class).getOptions();
			assertThat(options.getModel()).isEqualTo("qwen3.5:4b");
			assertThat(options.getThinkOption()).isEqualTo(ThinkOption.ThinkBoolean.DISABLED);
			assertThat(options.getNumCtx()).isEqualTo(8192);
		}
	}

	@Test
	void aTypoInTheSwitchFailsAtStartupNamingTheProperty() {
		// Before 7.4 a wrong spring.ai.model.chat left no ChatModel and failed with a NoSuchBeanDefinitionException for it. The validated
		// enum now fails first, on the property itself.
		assertThatThrownBy(() -> start("--app.ai.chat-provider=openai").close())
				.hasStackTraceContaining("app.ai.chat-provider")
				.hasStackTraceContaining("openai");
	}

	@Test
	void leavingTheSwitchUnsetFailsValidationBeforeTwoChatModelsCollide() {
		// spring.config.name=none skips application*.yml entirely, so the switch is really absent (not just empty). spring.ai.model.chat
		// is absent too, so both providers' autoconfig activates (matchIfMissing); the @NotNull on AiProperties fails first.
		assertThatThrownBy(() -> start("--spring.config.name=none", "--spring.ai.anthropic.api-key=test-key-not-used").close())
				.hasStackTraceContaining("app.ai")
				.hasStackTraceContaining("chatProvider");
	}

	@Test
	void aDeadOllamaFailsInSecondsNotMinutes() {
		// Unit 7.4: spring.ai.retry caps Spring AI's RetryTemplate (defaults: 10 attempts, uncapped 2 s x5 backoff, which hung for
		// minutes).
		// Nothing listens on port 1, so every attempt fails at connect time and only the retry schedule decides how long this takes.
		try (ConfigurableApplicationContext context = start("--app.ai.chat-provider=ollama",
															"--spring.ai.ollama.base-url=http://localhost:1")) {
			ChatModel chatModel = context.getBean(ChatModel.class);
			long start = System.nanoTime();

			assertThatThrownBy(() -> chatModel.call("Hi")).isInstanceOf(RuntimeException.class);

			assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(20));
		}
	}

	private static void assertSingleChatModel(final ConfigurableApplicationContext context, final Class<? extends ChatModel> type) {
		assertThat(context.getBeansOfType(ChatModel.class)).hasSize(1).allSatisfy((name, model) -> assertThat(model).isInstanceOf(type));
	}

	private static ConfigurableApplicationContext start(final String... args) {
		return run(new String[]{"test"}, args);
	}

	private static ConfigurableApplicationContext startWithProfiles(final String... profiles) {
		String[] all = new String[profiles.length + 1];
		all[0] = "test";
		System.arraycopy(profiles, 0, all, 1, profiles.length);
		return run(all);
	}

	private static ConfigurableApplicationContext run(final String[] profiles, final String... args) {
		// "test" profile first (dummy Anthropic key, quiet logs), then any profiles under test. No web server: the selection happens in
		// the
		// bean graph, not over HTTP.
		return new SpringApplicationBuilder(AiTrainingApplication.class).web(WebApplicationType.NONE).profiles(profiles).run(args);
	}
}