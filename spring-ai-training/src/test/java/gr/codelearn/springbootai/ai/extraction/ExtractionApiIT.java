package gr.codelearn.springbootai.ai.extraction;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 8.3 integration test: the full context over HTTP, with a {@code @Primary} scripted {@link ChatModel} whose options support native
 * structured output. It checks the whole path: the schema in the prompt or in the options, the repair round trip, Jakarta rules, and the
 * 502s when the output stays unusable.
 */
@AutoConfigureMockMvc
class ExtractionApiIT extends AbstractOracleIntegrationTest {

	private static final String VALID = """
										{"name": "Aurora X1", "category": "AUDIO", "price": 129.90, "currency": "EUR",
										 "features": ["40-hour battery", "USB-C"], "inStock": true}""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ScriptedChatModel chatModel;

	@BeforeEach
	void reset() {
		chatModel.reset();
	}

	@Test
	void promptModePutsTheSchemaInTheUserMessageAfterTheSystemPrompt() {
		chatModel.script(VALID);

		assertThat(post("""
						{"text": "Aurora X1 headphones, 40h battery, USB-C, 129.90 EUR, in stock."}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"product": {"name": "Aurora X1", "category": "AUDIO", "price": 129.90, "currency": "EUR",
									             "features": ["40-hour battery", "USB-C"], "inStock": true},
									 "outputMode": "prompt", "model": "stub-model",
									 "usage": {"promptTokens": 300, "completionTokens": 40, "totalTokens": 340}}""");

		final Prompt prompt = chatModel.prompts().getFirst();
		assertThat(prompt.getInstructions().getFirst()).isInstanceOf(SystemMessage.class);
		assertThat(prompt.getUserMessage().getText()).startsWith("Extract the product listing")
													 .contains("<text>", "Your response should be in JSON format.");
		assertThat(chatModel.outputSchemas().getFirst()).isNull();
	}

	@Test
	void nativeModeSendsTheSchemaAsAnOption() {
		chatModel.script(VALID);

		assertThat(post("""
						{"text": "Aurora X1 headphones.", "outputMode": "native"}""")).hasStatusOk()
																					  .bodyJson()
																					  .extractingPath("$.outputMode")
																					  .isEqualTo("native");

		assertThat(chatModel.prompts().getFirst().getUserMessage().getText()).doesNotContain("Your response should be in JSON format.");
		// The model is shown the schema as generated: optional properties stay non-null typed there.
		assertThat(chatModel.outputSchemas().getFirst()).contains("\"enum\" : [ \"AUDIO\"").doesNotContain("\"null\"");
	}

	@Test
	void aSchemaViolationIsRepairedByASecondCallAndTheUsageIsSummed() {
		chatModel.script("""
						 {"name": "Aurora X1", "category": "HEADPHONES", "features": []}""", VALID);

		assertThat(post("""
						{"text": "Aurora X1 headphones."}""")).hasStatusOk()
															  .bodyJson()
															  .extractingPath("$.usage.totalTokens")
															  .convertTo(Integer.class)
															  .isEqualTo(680);

		assertThat(chatModel.prompts()).hasSize(2);
		assertThat(chatModel.prompts().get(1).getUserMessage().getText()).contains("Output JSON validation failed because of:");
	}

	@Test
	void explicitNullsForOptionalPropertiesNeedNoRepair() {
		chatModel.script("""
						 {"name": "Nimbus lamp", "category": "HOME", "price": null, "currency": null, "features": [], "inStock": null}""");

		assertThat(post("""
						{"text": "Nimbus desk lamp, price on request."}""")).hasStatusOk()
																			.bodyJson()
																			.extractingPath("$.product.price")
																			.isNull();

		assertThat(chatModel.prompts()).hasSize(1);
	}

	@Test
	void outputThatStaysInvalidAfterTheRepairAttemptsIs502() {
		chatModel.script("not json", "still not json", "nope");

		assertThat(post("""
						{"text": "Aurora X1 headphones."}""")).hasStatus(HttpStatus.BAD_GATEWAY)
															  .bodyJson()
															  .extractingPath("$.errors[0].field")
															  .isEqualTo("$");

		assertThat(chatModel.prompts()).hasSize(1 + ExtractionService.REPAIR_ATTEMPTS);
	}

	@Test
	void schemaValidJsonThatBreaksABusinessRuleIs502WithTheFields() {
		chatModel.script("""
						 {"name": "Aurora X1", "category": "AUDIO", "price": -1, "currency": "eur", "features": []}""");

		assertThat(post("""
						{"text": "Aurora X1 headphones."}""")).hasStatus(HttpStatus.BAD_GATEWAY)
															  .bodyJson()
															  .extractingPath("$.errors[*].field")
															  .asArray()
															  .containsExactly("currency", "price");

		assertThat(chatModel.prompts()).hasSize(1);
	}

	@Test
	void modelFailureIs502() {
		chatModel.script();

		assertThat(post("""
						{"text": "Aurora X1 headphones."}""")).hasStatus(HttpStatus.BAD_GATEWAY)
															  .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	private MockMvcTester.MockMvcRequestBuilder post(final String body) {
		return mvc.post().uri("/api/v1/product-extractions").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@TestConfiguration
	static class StubModelConfig {

		@Bean
		@Primary
		ScriptedChatModel scriptedChatModel() {
			return new ScriptedChatModel();
		}
	}

	/**
	 * Replies with the scripted texts in order (an empty script simulates a provider outage) and records every prompt and the native
	 * output
	 * schema it carried.
	 */
	static class ScriptedChatModel implements ChatModel {

		private final Deque<String> replies = new ArrayDeque<>();
		private final List<Prompt> prompts = new ArrayList<>();
		private final List<@Nullable String> outputSchemas = new ArrayList<>();

		@Override
		public synchronized ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			outputSchemas.add(prompt.getOptions() instanceof StructuredOutputChatOptions options ? options.getOutputSchema() : null);
			if (replies.isEmpty()) {
				throw new IllegalStateException("simulated provider outage");
			}
			final ChatResponseMetadata metadata = ChatResponseMetadata.builder()
																	  .model("stub-model")
																	  .usage(new DefaultUsage(300, 40))
																	  .build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage(replies.pop()))), metadata);
		}

		@Override
		public ChatOptions getOptions() {
			return StructuredOutputChatOptions.builder().build();
		}

		synchronized void script(final String... texts) {
			replies.addAll(List.of(texts));
		}

		synchronized void reset() {
			replies.clear();
			prompts.clear();
			outputSchemas.clear();
		}

		synchronized List<Prompt> prompts() {
			return List.copyOf(prompts);
		}

		synchronized List<@Nullable String> outputSchemas() {
			return new ArrayList<>(outputSchemas);
		}
	}
}