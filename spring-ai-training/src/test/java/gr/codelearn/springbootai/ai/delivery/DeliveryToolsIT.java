package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ExternalServiceException.Kind;
import gr.codelearn.springbootai.ai.config.ai.ToolErrorProcessor;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.3 integration test: what a model is told, and what the API caller sees, when the tool's external dependency fails. The real
 * tools, validation proxy, {@link ToolErrorProcessor}, recording and catalog (Oracle, {@code knowledge-catalog.sql}) run; the model is
 * scripted and the carrier is replaced by {@link FailingCarrier}, which fails the way each test says. The carrier's own HTTP behavior is
 * covered by {@code HttpCarrierQuoteClientTest}.
 */
@AutoConfigureMockMvc
@Sql("/sql/knowledge-catalog.sql")
class DeliveryToolsIT extends AbstractOracleIntegrationTest {

	private static final String DOWN = "The delivery service is not responding (3 attempts). " + Kind.UNAVAILABLE.advice();

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ScriptedModel model;

	@Autowired
	private FailingCarrier carrier;

	@BeforeEach
	void reset() {
		model.prompts.clear();
		carrier.calls = 0;
		carrier.failure = null;
	}

	@Test
	void aCarrierQuoteBecomesTheToolsResult() {
		final var response = ask("How long to deliver LAP-0002 to 10431?").exchange();

		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"toolCalls": [{"name": "estimateDelivery", "error": null}]}""");
		assertThat(response).bodyJson().extractingPath("$.content").asString()
							.startsWith("Tool said: {\"sku\":\"LAP-0002\",\"postcode\":\"10431\",\"days\":3");
	}

	@Test
	void aCarrierThatIsDownReachesTheModelAsWhatFailedAndWhatToDo() {
		carrier.failure = () -> new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service is not responding (3 attempts)",
															 new IllegalStateException("connect timed out: 10.1.2.3:8443"));

		final var response = ask("How long to deliver LAP-0002 to 50000?").exchange();

		// The model's second round got the message, in words: not a stack trace, not a status code, and no host from the cause.
		assertThat(model.prompts).hasSize(2);
		final var toolResponse = (ToolResponseMessage) model.prompts.getLast().getInstructions().getLast();
		assertThat(toolResponse.getResponses().getFirst().responseData()).isEqualTo(DOWN);
		// The API response shows the same text as the tool call's error: what the model was told.
		assertThat(response).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																		 {"content": "Tool said: %s",
																		  "toolCalls": [{"name": "estimateDelivery", "error": "%s"}]}""".formatted(
				DOWN, DOWN));
		assertThat(response).bodyText().doesNotContain("10.1.2.3");
	}

	@Test
	void aRefusedPostcodeTellsTheModelNotToRetry() {
		carrier.failure = () -> new ExternalServiceException(Kind.REJECTED, "The carrier does not deliver to postcode 40404", null);

		assertThat(ask("How long to deliver LAP-0002 to 40404?")).hasStatusOk().bodyJson().extractingPath("$.content").asString()
																 .contains("does not deliver to postcode 40404")
																 .contains("Retrying with the same input will not help");
	}

	@Test
	void anUnexpectedFailureNeverLeaksItsMessageToTheModelOrTheCaller() {
		carrier.failure = () -> new IllegalStateException(
				"ORA-00942: table or view does not exist, jdbc:oracle:thin:@db.internal:1521/prod");

		final var response = ask("How long to deliver LAP-0002 to 10431?").exchange();

		final var toolResponse = (ToolResponseMessage) model.prompts.getLast().getInstructions().getLast();
		assertThat(toolResponse.getResponses().getFirst().responseData()).isEqualTo(ToolErrorProcessor.UNEXPECTED_MESSAGE);
		assertThat(response).hasStatusOk().bodyJson().extractingPath("$.toolCalls[0].error").asString()
							.isEqualTo(ToolErrorProcessor.UNEXPECTED_MESSAGE);
		assertThat(response).bodyText().doesNotContain("ORA-00942").doesNotContain("db.internal");
	}

	@Test
	void anUnknownProductIsAFactTheModelReadsAndTheCarrierIsNeverAsked() {
		assertThat(ask("How long to deliver AUD-9999 to 10431?")).hasStatusOk().bodyJson().extractingPath("$.content").asString()
																 .isEqualTo("Tool said: No product with SKU AUD-9999");
		assertThat(carrier.calls).isZero();
	}

	@Test
	void aBadPostcodeIsRejectedBeforeTheCarrierIsAsked() {
		assertThat(ask("How long to deliver LAP-0002 to 104?")).hasStatusOk().bodyJson().extractingPath("$.content").asString()
															   .contains("Invalid arguments, the tool did not run")
															   .contains("postcode must be exactly five digits");
		assertThat(carrier.calls).isZero();
	}

	private MockMvcTester.MockMvcRequestBuilder ask(final String question) {
		return mvc.post()
				  .uri("/api/v1/assistant/answers")
				  .contentType(MediaType.APPLICATION_JSON)
				  .content("{\"question\": \"" + question + "\"}");
	}

	@TestConfiguration
	static class DeliveryTestConfig {

		@Bean
		@Primary
		ScriptedModel scriptedModel() {
			return new ScriptedModel();
		}

		@Bean
		@Primary
		FailingCarrier failingCarrier() {
			return new FailingCarrier();
		}
	}

	/**
	 * Stands in for the carrier: a quote, unless a test set a {@code failure}. Counts the calls it gets.
	 */
	static final class FailingCarrier implements CarrierQuoteClient {

		private volatile Supplier<RuntimeException> failure;
		private volatile int calls;

		@Override
		public Quote quote(final String postcode) {
			calls++;
			if (failure != null) {
				throw failure.get();
			}
			return new Quote(3, new BigDecimal("4.90"), "Stub Express");
		}
	}

	/**
	 * Round 1: asks for {@code estimateDelivery} with the SKU (default LAP-0002) and the five-digit number of the question, or with what
	 * follows "to " when that is not five digits. Round 2 (a tool response is present): answers with the tool's result.
	 */
	static final class ScriptedModel implements ChatModel {

		private static final Pattern SKU = Pattern.compile("[A-Z]{3}-\\d{4}");
		private static final Pattern POSTCODE = Pattern.compile("to (\\d+)");

		private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			final Message last = prompt.getInstructions().getLast();
			final AssistantMessage reply;
			if (last instanceof ToolResponseMessage toolResponse) {
				reply = new AssistantMessage("Tool said: " + toolResponse.getResponses().getFirst().responseData());
			} else {
				final String question = prompt.getUserMessage().getText();
				final var sku = SKU.matcher(question);
				final var postcode = POSTCODE.matcher(question);
				final String arguments = "{\"input\":{\"sku\":\"%s\",\"postcode\":\"%s\"}}".formatted(sku.find() ? sku.group() : "LAP" +
																																 "-0002",
																									  postcode.find() ? postcode.group(
						                                                                                      1) : "10431");
				reply = AssistantMessage.builder()
										.content("")
										.toolCalls(List.of(new ToolCall("call-1", "function", "estimateDelivery", arguments)))
										.build();
			}
			return new ChatResponse(List.of(new Generation(reply)),
									ChatResponseMetadata.builder().model("scripted").usage(new DefaultUsage(100, 10)).build());
		}

		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}
	}
}