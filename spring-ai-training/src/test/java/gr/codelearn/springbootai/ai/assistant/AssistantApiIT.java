package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.1 integration test: the tool loop end to end, with the real {@code CatalogTools} over Oracle (the {@code knowledge-catalog.sql}
 * fixture) and Spring AI's tool calling advisor. Only the model is scripted: {@link ScriptedModel} asks for {@code getProduct} with the SKU
 * found in the question, then answers with the tool result it received. So the test sees exactly what a real model would get.
 */
@AutoConfigureMockMvc
@Sql("/sql/knowledge-catalog.sql")
class AssistantApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ScriptedModel model;

	@BeforeEach
	void reset() {
		model.prompts.clear();
	}

	@Test
	void theModelsToolCallRunsAgainstTheCatalogAndItsResultGoesBackToTheModel() {
		// Exchanged once: asserting on the request builder twice would send it twice.
		final var response = ask("What does LAP-0002 cost?").exchange();

		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"toolCalls": [{"name": "getProduct", "arguments": "{\\"sku\\":\\"LAP-0002\\"}", "error": null}],
																		  "usage": {"promptTokens": 200, "completionTokens": 20, "totalTokens": 220}}""");
		// The tool result the model got is the product as JSON, read live from the catalog. Usage is summed over both rounds.
		assertThat(response).bodyJson().extractingPath("$.content").asString()
							.startsWith("Tool said: {\"sku\":\"LAP-0002\",\"name\":\"Latitude 16 Workstation\"")
							.contains("\"price\":2199.00");

		// Two rounds: the question (with the tool definitions), then the question + the tool call + its result.
		assertThat(model.prompts).hasSize(2);
		final ToolCallingChatOptions options = (ToolCallingChatOptions) model.prompts.getFirst().getOptions();
		assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name())
											  .containsExactlyInAnyOrder("searchProducts", "getProduct");
		assertThat(model.prompts.getLast().getInstructions()).extracting(Message::getMessageType).map(Enum::name)
															 .containsExactly("SYSTEM", "USER", "ASSISTANT", "TOOL");
	}

	@Test
	void aFailingToolCallReachesTheModelAsItsMessageNotAsAnError() {
		assertThat(ask("Is AUD-9999 in stock?")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																							 {"content": "Tool said: No product with SKU AUD-9999",
																							  "toolCalls": [{"name": "getProduct", "resultChars": 28, "error": "No product with SKU AUD-9999"}]}""");
	}

	@Test
	void withoutASkuTheModelAnswersWithoutTools() {
		assertThat(ask("What is the capital of France?")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																									  {"content": "No tool needed.", "toolCalls": [], "usage": {"totalTokens": 110}}""");
		assertThat(model.prompts).hasSize(1);
	}

	private MockMvcTester.MockMvcRequestBuilder ask(final String question) {
		return mvc.post()
				  .uri("/api/v1/assistant/answers")
				  .contentType(MediaType.APPLICATION_JSON)
				  .content("{\"question\": \"" + question + "\"}");
	}

	@TestConfiguration
	static class ScriptedModelConfig {

		@Bean
		@Primary
		ScriptedModel scriptedModel() {
			return new ScriptedModel();
		}
	}

	/**
	 * Round 1: a SKU in the user message → a {@code getProduct} tool call; none → a plain answer. Round 2 (a tool response is present):
	 * answers with the tool's result. 100 prompt + 10 completion tokens per round.
	 */
	static final class ScriptedModel implements ChatModel {

		private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			final Message last = prompt.getInstructions().getLast();
			final AssistantMessage reply;
			if (last instanceof ToolResponseMessage toolResponse) {
				reply = new AssistantMessage("Tool said: " + toolResponse.getResponses().getFirst().responseData());
			} else {
				final var sku = java.util.regex.Pattern.compile("[A-Z]{3}-\\d{4}").matcher(prompt.getUserMessage().getText());
				reply = sku.find()
						? AssistantMessage.builder()
										  .content("")
										  .toolCalls(List.of(new ToolCall("call-1", "function", "getProduct",
																		  "{\"sku\":\"" + sku.group() + "\"}")))
										  .build()
						: new AssistantMessage("No tool needed.");
			}
			return new ChatResponse(List.of(new Generation(reply)),
									ChatResponseMetadata.builder().model("scripted").usage(new DefaultUsage(100, 10)).build());
		}

		@Override
		public ChatOptions getOptions() {
			// Pitfall: the ChatClient only attaches tools when the model's options are ToolCallingChatOptions (every real provider's
			// are). With the default plain ChatOptions, .tools(...) is dropped silently and the model never sees a tool.
			return ToolCallingChatOptions.builder().build();
		}
	}
}