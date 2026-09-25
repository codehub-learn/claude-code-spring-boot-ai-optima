package gr.codelearn.springbootai.ai.writing;

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
 * Unit 8.1 integration test: the full context over HTTP, with a {@code @Primary} stub {@link ChatModel} in place of the provider. It checks
 * what actually reaches the model: the application's system prompt first, then the use case's rendered template as the user message. Plus
 * the failure path (502).
 */
@AutoConfigureMockMvc
class WritingApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RecordingChatModel chatModel;

	@Test
	void summaryTemplateIsRenderedIntoTheUserMessageAfterTheSystemPrompt() {
		assertThat(mvc.post().uri("/api/v1/summaries").contentType(MediaType.APPLICATION_JSON).content("""
																									   {"text": "Spring AI 2.0 is out.", "maxWords": 20,
																									    "audience": "managers"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "A short text.", "model": "stub-model",
									 "usage": {"promptTokens": 90, "completionTokens": 10, "totalTokens": 100}}""");

		final List<Message> messages = chatModel.lastPrompt().getInstructions();
		assertThat(messages).satisfiesExactly(
				system -> assertThat(system).isInstanceOf(SystemMessage.class)
											.extracting(Message::getText)
											.asString()
											.endsWith("Today is " + LocalDate.now() + "."),
				user -> assertThat(user).isInstanceOf(UserMessage.class)
										.extracting(Message::getText)
										.asString()
										.startsWith("Summarize the text below in at most 20 words.")
										.contains("audience: managers")
										.contains("Spring AI 2.0 is out."));
	}

	@Test
	void productDescriptionTemplateListsTheFeaturesInTheUserMessage() {
		assertThat(mvc.post().uri("/api/v1/product-descriptions").contentType(MediaType.APPLICATION_JSON).content("""
																												  {"name": "Aurora X1", "category": "Audio",
																												   "features": ["40-hour battery", "USB-C"],
																												   "tone": "friendly"}"""))
				.hasStatusOk();

		// ST writes line breaks as the platform's separator (\r\n on Windows); compare with \n.
		assertThat(chatModel.lastPrompt().getUserMessage().getText().replace("\r\n", "\n")).contains(
																								   "Key features:\n- 40-hour battery\n- " +
																								   "USB-C\n")
																						   .contains("Tone: friendly.")
																						   .contains("Language: English.");
	}

	@Test
	void modelFailureIsMappedTo502() {
		assertThat(mvc.post().uri("/api/v1/summaries").contentType(MediaType.APPLICATION_JSON).content("""
																									   {"text": "FAIL please", "maxWords": 20}"""))
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
																	  .usage(new DefaultUsage(90, 10))
																	  .build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("A short text."))), metadata);
		}

		Prompt lastPrompt() {
			return lastPrompt.get();
		}
	}
}