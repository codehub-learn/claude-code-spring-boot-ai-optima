package gr.codelearn.springbootai.ai.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Units 6.2 - 6.4 integration test: the full context (Anthropic starter, {@code config.ai.ChatClientConfig}, the advisor chain,
 * {@code config.web.ApiExceptionHandler}, API versioning, springdoc) over HTTP via {@link MockMvcTester}. Only the provider is replaced: a
 * {@code @Primary} stub {@link ChatModel} stands in for {@code AnthropicChatModel}, so no API key or network is needed. Covers the blocking
 * and the streamed chat completion and the message-list endpoint, each with a model failure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatApiIT {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private StubChatModel stubChatModel;

	@Test
	void chatCompletionFlowsThroughTheRealChatClientToTheModel() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "Say hi"}"""))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
									{"content": "echo: Say hi", "model": "stub-model",
									 "usage": {"promptTokens": 12, "completionTokens": 5, "totalTokens": 17}}""");
	}

	@Test
	void modelFailureIsMappedTo502() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "FAIL please"}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson()
				.extractingPath("$.status")
				.convertTo(Integer.class)
				.isEqualTo(502);
	}

	@Test
	void openApiSpecIsStillServedAlongsideTheVersionedApi() {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.paths")
				.asMap()
				.containsKey("/api/v1/chat-completions");
	}

	@Test
	void streamedChatCompletionArrivesAsChunkEvents() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
					  .content("""
							   {"message": "Say hi"}"""))
				.hasStatusOk()
				.bodyText()
				.isEqualTo("""
						   event:chunk
						   data:{"content":"echo:"}
						   
						   event:chunk
						   data:{"content":" Say hi"}
						   
						   """);
	}

	@Test
	void streamedModelFailureEndsWithAnErrorEvent() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM)
					  .content("""
							   {"message": "FAIL please"}"""))
				.hasStatusOk()
				.bodyText()
				.startsWith("event:error\ndata:{")
				.contains("\"status\":502");
	}

	@Test
	void promptMessagesReachTheModelAsTypedMessagesInOrder() {
		assertThat(mvc.post().uri("/api/v1/prompts").contentType(MediaType.APPLICATION_JSON).content("""
																									 {"messages": [
																									   {"role": "user",      "content": "Capital of Greece?"},
																									   {"role": "assistant", "content": "Athens."},
																									   {"role": "user",      "content": "And of Italy?"}]}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.content")
				.isEqualTo("echo: And of Italy?");

		final List<Message> instructions = stubChatModel.lastPrompt().getInstructions();
		// 6.4: the application's system prompt comes first, then the client's turns, in order.
		assertThat(instructions.getFirst().getMessageType()).isEqualTo(MessageType.SYSTEM);
		assertThat(instructions.subList(1, instructions.size()))
				.extracting(Message::getMessageType, Message::getText)
				.containsExactly(tuple(MessageType.USER, "Capital of Greece?"), tuple(MessageType.ASSISTANT, "Athens."),
								 tuple(MessageType.USER, "And of Italy?"));
	}

	@Test
	void everyCallStartsWithTheApplicationSystemPromptRenderedFromTheTemplate() {
		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "Say hi"}"""))
				.hasStatusOk();

		assertThat(stubChatModel.lastPrompt().getInstructions()).satisfiesExactly(
				system -> assertThat(system).isInstanceOf(SystemMessage.class)
											.extracting(Message::getText)
											.asString()
											.startsWith("You are the Code.Learn training assistant.")
											.endsWith("Today is " + LocalDate.now() + ".")
											.doesNotContain("{today}"),
				user -> assertThat(user).isInstanceOf(UserMessage.class).extracting(Message::getText).isEqualTo("Say hi"));
	}

	@Test
	void promptModelFailureIsMappedTo502() {
		assertThat(mvc.post().uri("/api/v1/prompts").contentType(MediaType.APPLICATION_JSON).content("""
																									 {"messages": [{"role": "user", "content": "FAIL please"}]}"""))
				.hasStatus(HttpStatus.BAD_GATEWAY);
	}

	@TestConfiguration
	static class StubModelConfig {

		@Bean
		@Primary
		StubChatModel stubChatModel() {
			return new StubChatModel();
		}
	}

	/**
	 * Echoes the last user message back with fixed metadata; a message containing "FAIL" simulates a provider outage. {@code stream} emits
	 * the same echo in two fragments. Remembers the last {@link Prompt} so a test can check what actually reached the model.
	 */
	static class StubChatModel implements ChatModel {

		private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			final String userText = userText(prompt);
			final ChatResponseMetadata metadata = ChatResponseMetadata.builder()
																	  .model("stub-model")
																	  .usage(new DefaultUsage(12, 5))
																	  .build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("echo: " + userText))), metadata);
		}

		@Override
		public Flux<ChatResponse> stream(final Prompt prompt) {
			// Fails on subscription, like a real provider whose HTTP call is only made once the stream is subscribed.
			return Flux.defer(() -> Flux.just("echo:", " " + userText(prompt)))
					   .map(text -> new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
		}

		Prompt lastPrompt() {
			return lastPrompt.get();
		}

		private String userText(final Prompt prompt) {
			lastPrompt.set(prompt);
			final String userText = prompt.getUserMessage().getText();
			if (userText.contains("FAIL")) {
				throw new IllegalStateException("simulated provider outage");
			}
			return userText;
		}
	}
}