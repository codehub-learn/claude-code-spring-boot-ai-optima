package gr.codelearn.springbootai.ai.answer;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 8.2 integration test: the full context over HTTP, with a {@code @Primary} stub {@link ChatModel} in place of the provider. It checks
 * what actually reaches the model: the application's system prompt unchanged, then one user message carrying the instructions, the
 * documents and the question in that order. Plus the failure path (502).
 */
@AutoConfigureMockMvc
class AnswerApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RecordingChatModel chatModel;

	@Test
	void documentsTravelInTheUserMessageBetweenTheInstructionsAndTheQuestion() {
		assertThat(mvc.post().uri("/api/v1/answers").contentType(MediaType.APPLICATION_JSON).content("""
																									 {"question": "How long is the warranty?",
																									  "documents": [{"title": "Warranty",
																									                 "text": "Two years."}]}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "An answer.", "model": "stub-model",
									 "usage": {"promptTokens": 150, "completionTokens": 5, "totalTokens": 155}}""");

		final List<Message> messages = chatModel.lastPrompt().getInstructions();
		assertThat(messages).satisfiesExactly(
				system -> assertThat(system).isInstanceOf(SystemMessage.class)
											.extracting(Message::getText)
											.asString()
											.startsWith("You are the Code.Learn training assistant.")
											.endsWith("Today is " + LocalDate.now() + ".")
											.doesNotContain("<documents>"),
				user -> assertThat(user).isInstanceOf(UserMessage.class)
										.extracting(message -> message.getText().replace("\r\n", "\n"))
										.asString()
										.startsWith("<instructions>\n")
										.contains("Use only the documents below.")
										.contains("<document number=\"1\" title=\"Warranty\">\nTwo years.\n</document>")
										.endsWith("<question>\nHow long is the warranty?\n</question>"));
	}

	@Test
	void withoutDocumentsTheUserMessageIsInstructionOnly() {
		assertThat(mvc.post().uri("/api/v1/answers").contentType(MediaType.APPLICATION_JSON).content("""
																									 {"question": "What is Spring AI?"}"""))
				.hasStatusOk();

		assertThat(chatModel.lastPrompt().getUserMessage().getText()).contains("No documents were provided.").doesNotContain("<documents" +
																															 ">");
	}

	@Test
	void modelFailureIsMappedTo502() {
		assertThat(mvc.post().uri("/api/v1/answers").contentType(MediaType.APPLICATION_JSON).content("""
																									 {"question": "FAIL please"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@TestConfiguration
	static class StubModelConfig {

		@Bean
		@Primary
		RecordingChatModel recordingChatModel() {
			return new RecordingChatModel();
		}
	}

	/**
	 * Answers with fixed text and metadata and remembers the last {@link Prompt}; a user message containing "FAIL" simulates a provider
	 * outage.
	 */
	static class RecordingChatModel implements ChatModel {

		private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			lastPrompt.set(prompt);
			if (prompt.getUserMessage().getText().contains("FAIL")) {
				throw new IllegalStateException("simulated provider outage");
			}
			final ChatResponseMetadata metadata = ChatResponseMetadata.builder()
																	  .model("stub-model")
																	  .usage(new DefaultUsage(150, 5))
																	  .build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("An answer."))), metadata);
		}

		Prompt lastPrompt() {
			return lastPrompt.get();
		}
	}
}