package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
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

	@Autowired
	private JdbcClient jdbcClient;

	private String lastAnswer;

	@BeforeEach
	void reset() {
		model.prompts.clear();
	}

	@Test
	void theModelsToolCallRunsAgainstTheCatalogAndItsResultGoesBackToTheModel() {
		// Exchanged once: asserting on the request builder twice would send it twice.
		final var response = ask("What does LAP-0002 cost?").exchange();

		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"toolCalls": [{"name": "getProduct", "arguments": "{\\"input\\":{\\"sku\\":\\"LAP-0002\\"}}", "error": null}],
																		  "usage": {"promptTokens": 200, "completionTokens": 20, "totalTokens": 220}}""");
		// The tool result the model got is the product as JSON, read live from the catalog. Usage is summed over both rounds.
		assertThat(response).bodyJson().extractingPath("$.content").asString()
							.startsWith("Tool said: {\"sku\":\"LAP-0002\",\"name\":\"Latitude 16 Workstation\"")
							.contains("\"price\":2199.00");

		// Two rounds: the question (with the tool definitions), then the question + the tool call + its result.
		assertThat(model.prompts).hasSize(2);
		final ToolCallingChatOptions options = (ToolCallingChatOptions) model.prompts.getFirst().getOptions();
		assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name())
											  .containsExactlyInAnyOrder("searchProducts", "getProduct", "estimateDelivery");
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
	void aRejectedCallReachesTheModelAsAValidationMessageAndTheModelCorrectsIt() {
		final var response = ask("Tell me about the blue laptop").exchange();

		// Three rounds: the bad call (a product name where a SKU belongs), its rejection, the corrected call and its answer.
		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"toolCalls": [
																		   {"name": "getProduct", "arguments": "{\\"input\\":{\\"sku\\":\\"blue laptop\\"}}"},
																		   {"name": "getProduct", "arguments": "{\\"input\\":{\\"sku\\":\\"LAP-0002\\"}}", "error": null}]}""");
		assertThat(response).bodyJson().extractingPath("$.toolCalls[0].error").asString()
							.isEqualTo("Invalid arguments, the tool did not run: sku must be letters, a dash and digits, like LAP-0002. "
									   + "Fix the arguments and call the tool again.");
		assertThat(response).bodyJson().extractingPath("$.content").asString().startsWith("Tool said: {\"sku\":\"LAP-0002\"");

		// The model saw the rejection as the tool's result, in words, and the catalog was not queried for "blue laptop".
		assertThat(model.prompts).hasSize(3);
		final var rejection = (ToolResponseMessage) model.prompts.get(1).getInstructions().getLast();
		assertThat(rejection.getResponses().getFirst().responseData())
				.isEqualTo("Invalid arguments, the tool did not run: sku must be letters, a dash and digits, like LAP-0002. "
						   + "Fix the arguments and call the tool again.");
	}

	@Test
	void theShopInstructionsAreTheSystemPromptAndTheUserMessageIsJustTheQuestion() {
		ask("What is the capital of France?").exchange();

		final Prompt prompt = model.prompts.getFirst();
		assertThat(prompt.getSystemMessage().getText()).contains("You are the Code.Learn training assistant")
													   .contains("shop assistant of an electronics store")
													   .contains("estimateDelivery")
													   .doesNotContain("{today}");
		assertThat(prompt.getUserMessage().getText()).isEqualTo("What is the capital of France?");
	}

	@Test
	void aFollowUpInTheSameConversationSeesTheEarlierMessages() {
		final var first = ask("What does LAP-0002 cost?").exchange();
		final String conversation = conversationOf(first);
		final int roundsSoFar = model.prompts.size();

		final var second = ask("And what about AUD-9999?", conversation).exchange();

		assertThat(conversationOf(second)).isEqualTo(conversation);
		// The second turn's first round: system, then what was said before (question + answer), then the new question.
		final Prompt followUp = model.prompts.get(roundsSoFar);
		assertThat(followUp.getInstructions()).extracting(Message::getMessageType).map(Enum::name)
											  .containsExactly("SYSTEM", "USER", "ASSISTANT", "USER");
		assertThat(followUp.getInstructions().get(1).getText()).isEqualTo("What does LAP-0002 cost?");
		assertThat(followUp.getInstructions().get(2).getText()).startsWith("Tool said: {\"sku\":\"LAP-0002\"");
		assertThat(followUp.getUserMessage().getText()).isEqualTo("And what about AUD-9999?");
	}

	@Test
	void memoryHoldsTheQuestionsAndTheAnswersNotTheInstructionsOrTheToolMessages() {
		final String conversation = conversationOf(ask("What does LAP-0002 cost?").exchange());

		final var history = mvc.get().uri("/api/v1/conversations/{id}/messages", conversation).exchange();

		// Two messages: the question as asked (no instruction block) and the final answer (no tool call or tool response message).
		assertThat(history).hasStatusOk().bodyJson().extractingPath("$.length()").isEqualTo(2);
		assertThat(history).bodyJson().extractingPath("$[0].role").isEqualTo("user");
		assertThat(history).bodyJson().extractingPath("$[0].content").isEqualTo("What does LAP-0002 cost?");
		assertThat(history).bodyJson().extractingPath("$[1].role").isEqualTo("assistant");
		assertThat(history).bodyJson().extractingPath("$[1].content").isEqualTo(lastAnswer);
	}

	@Test
	void theCookieOfAnAnswerContinuesTheConversationWithoutAnyIdInTheBody() {
		final var first = ask("What does LAP-0002 cost?").exchange();
		final Cookie cookie = first.getResponse().getCookie("assistant-conversation");
		assertThat(cookie).isNotNull();
		assertThat(cookie.getValue()).isEqualTo(conversationOf(first));
		assertThat(cookie.isHttpOnly()).isTrue();
		final int roundsSoFar = model.prompts.size();

		// The way Swagger UI or IntelliJ's HTTP client sends the next message: no conversationId, the cookie comes back by itself.
		final var second = ask("And what about AUD-9999?").cookie(cookie).exchange();

		assertThat(conversationOf(second)).isEqualTo(cookie.getValue());
		assertThat(model.prompts.get(roundsSoFar).getInstructions()).extracting(Message::getMessageType).map(Enum::name)
																	.containsExactly("SYSTEM", "USER", "ASSISTANT", "USER");
	}

	@Test
	void aCookieForADeletedConversationStartsANewOne() {
		final var first = ask("What is the capital of France?").exchange();
		final Cookie cookie = first.getResponse().getCookie("assistant-conversation");
		assertThat(mvc.delete().uri("/api/v1/assistant/conversation").cookie(cookie)).hasStatus(204);
		assertThat(mvc.get().uri("/api/v1/conversations/{id}/messages", cookie.getValue())).hasStatus(404);

		final var again = ask("And of Spain?").cookie(cookie).exchange();

		assertThat(conversationOf(again)).isNotEqualTo(cookie.getValue());
		assertThat(model.prompts.getLast().getInstructions()).extracting(Message::getMessageType).map(Enum::name)
															 .containsExactly("SYSTEM", "USER");
	}

	@Test
	void withoutAConversationIdEachMessageStartsANewConversationWithoutHistory() {
		final String one = conversationOf(ask("What is the capital of France?").exchange());
		final String two = conversationOf(ask("And of Spain?").exchange());

		assertThat(two).isNotEqualTo(one);
		assertThat(model.prompts.getLast().getInstructions()).extracting(Message::getMessageType).map(Enum::name)
															 .containsExactly("SYSTEM", "USER");
	}

	@Test
	void anUnknownConversationIsA404AndTheModelIsNotCalled() {
		assertThat(ask("Hello again", "6f0d3b0e-0000-4000-8000-000000000000")).hasStatus(404);

		assertThat(model.prompts).isEmpty();
	}

	@Test
	void aFailedFirstMessageLeavesNoConversationBehind() {
		final int before = conversations();

		assertThat(ask("Please explode boom")).hasStatus(502);

		assertThat(conversations()).isEqualTo(before);
	}

	@Test
	void aFailedFollowUpLeavesTheMemoryAsItWas() {
		final String conversation = conversationOf(ask("What is the capital of France?").exchange());

		assertThat(ask("Please explode boom", conversation)).hasStatus(502);

		assertThat(mvc.get().uri("/api/v1/conversations/{id}/messages", conversation)).hasStatusOk().bodyJson()
																					  .extractingPath("$.length()").isEqualTo(2);
	}

	private String conversationOf(final MvcTestResult response) {
		assertThat(response).hasStatusOk();
		final String body = new String(response.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
		lastAnswer = JsonPath.read(body, "$.content");
		return JsonPath.read(body, "$.conversationId");
	}

	private int conversations() {
		return jdbcClient.sql("select count(*) from conversation").query(Integer.class).single();
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

	private MockMvcTester.MockMvcRequestBuilder ask(final String question, final String conversationId) {
		return mvc.post()
				  .uri("/api/v1/assistant/answers")
				  .contentType(MediaType.APPLICATION_JSON)
				  .content("{\"question\": \"" + question + "\", \"conversationId\": \"" + conversationId + "\"}");
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
	 * Round 1: a SKU in the user message -> a {@code getProduct} tool call; "blue laptop" -> a call with that name as the SKU (invalid);
	 * none -> a plain answer. Next rounds (a tool response is present): after a rejection, the call again with a valid SKU; otherwise
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
				final String result = String.valueOf(toolResponse.getResponses().getFirst().responseData());
				reply = result.startsWith("Invalid arguments") ? getProduct("LAP-0002") : new AssistantMessage("Tool said: " + result);
			} else {
				final String question = prompt.getUserMessage().getText();
				if (question.contains("boom")) {
					throw new IllegalStateException("the model exploded");
				}
				final var sku = java.util.regex.Pattern.compile("[A-Z]{3}-\\d{4}").matcher(question);
				if (sku.find()) {
					reply = getProduct(sku.group());
				} else if (question.contains("blue laptop")) {
					reply = getProduct("blue laptop");
				} else {
					reply = new AssistantMessage("No tool needed.");
				}
			}
			return new ChatResponse(List.of(new Generation(reply)),
									ChatResponseMetadata.builder().model("scripted").usage(new DefaultUsage(100, 10)).build());
		}

		private static AssistantMessage getProduct(final String sku) {
			return AssistantMessage.builder()
								   .content("")
								   .toolCalls(List.of(new ToolCall("call-1", "function", "getProduct",
																   "{\"input\":{\"sku\":\"" + sku + "\"}}")))
								   .build();
		}

		@Override
		public ChatOptions getOptions() {
			// Pitfall: the ChatClient only attaches tools when the model's options are ToolCallingChatOptions (every real provider's
			// are). With the default plain ChatOptions, .tools(...) is dropped silently and the model never sees a tool.
			return ToolCallingChatOptions.builder().build();
		}
	}
}