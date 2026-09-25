package gr.codelearn.springbootai.ai.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 9.1 integration test: the full context over HTTP, with a {@code @Primary} stub {@link ChatModel} that answers "Reply n" and records
 * every prompt. It proves the difference that matters: on a stateful turn the model receives the earlier turns, on a stateless call it
 * receives only what the request carries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConversationApiIT {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RecordingChatModel chatModel;

	@BeforeEach
	void reset() {
		chatModel.reset();
	}

	@Test
	void eachTurnSendsTheWholeHistoryBack() {
		final String messages = create() + "/messages";

		send(messages, "My name is Maria.").hasStatusOk().bodyJson().extractingPath("$.turn").convertTo(Integer.class).isEqualTo(1);
		send(messages, "What is my name?").hasStatusOk()
										  .bodyJson()
										  .isLenientlyEqualTo("""
															  {"turn": 2, "content": "Reply 2"}""");

		assertThat(roles(chatModel.prompts().get(0))).containsExactly(MessageType.SYSTEM, MessageType.USER);
		final List<Message> second = chatModel.prompts().get(1).getInstructions();
		assertThat(roles(chatModel.prompts().get(1))).containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT,
																	  MessageType.USER);
		assertThat(second.subList(1, 4)).extracting(Message::getText).containsExactly("My name is Maria.", "Reply 1", "What is my name?");
		assertThat(mvc.get().uri(messages)).hasStatusOk().bodyJson().isEqualTo("""
																			   [{"role": "user", "content": "My name is Maria."},
																			    {"role": "assistant", "content": "Reply 1"},
																			    {"role": "user", "content": "What is my name?"},
																			    {"role": "assistant", "content": "Reply 2"}]""");
	}

	@Test
	void aStatelessCallSeesNoHistory() {
		send(create() + "/messages", "My name is Maria.").hasStatusOk();

		assertThat(mvc.post().uri("/api/v1/chat-completions").contentType(MediaType.APPLICATION_JSON).content("""
																											  {"message": "What is my name?"}"""))
				.hasStatusOk();

		assertThat(roles(chatModel.prompts().getLast())).containsExactly(MessageType.SYSTEM, MessageType.USER);
	}

	@Test
	void conversationsAreIsolated() {
		send(create() + "/messages", "My name is Maria.").hasStatusOk();
		send(create() + "/messages", "What is my name?").hasStatusOk();

		assertThat(roles(chatModel.prompts().getLast())).containsExactly(MessageType.SYSTEM, MessageType.USER);
	}

	@Test
	void aFailedTurnIsNotStored() {
		final String messages = create() + "/messages";

		send(messages, "FAIL please").hasStatus(HttpStatus.BAD_GATEWAY);

		assertThat(mvc.get().uri(messages)).hasStatusOk().bodyJson().isEqualTo("[]");
	}

	@Test
	void aConversationHasATurnLimit() {
		final String messages = create() + "/messages";
		IntStream.rangeClosed(1, ConversationService.MAX_TURNS).forEach(i -> send(messages, "Turn " + i).hasStatusOk());

		send(messages, "One more").hasStatus(HttpStatus.CONFLICT);
		assertThat(chatModel.prompts()).hasSize(ConversationService.MAX_TURNS);
	}

	@Test
	void aDeletedConversationIsGone() {
		final String conversation = create();

		assertThat(mvc.delete().uri(conversation)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri(conversation + "/messages")).hasStatus(HttpStatus.NOT_FOUND);
		send(conversation + "/messages", "Hello?").hasStatus(HttpStatus.NOT_FOUND);
		assertThat(chatModel.prompts()).isEmpty();
	}

	/**
	 * Creates a conversation and returns its path, taken from the {@code Location} header.
	 */
	private String create() {
		final MvcTestResult result = mvc.post().uri("/api/v1/conversations").exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		final String location = Objects.requireNonNull(result.getResponse().getHeader("Location"));
		final String path = URI.create(location).getPath();
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(path.substring(path.lastIndexOf('/') + 1));
		return path;
	}

	private MvcTestResultAssert send(final String messages, final String content) {
		return assertThat(mvc.post().uri(messages).contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"" + content + "\"}"));
	}

	private static List<MessageType> roles(final Prompt prompt) {
		return prompt.getInstructions().stream().map(Message::getMessageType).toList();
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
	 * Answers "Reply n" (n counts the calls) and remembers every {@link Prompt}; a user message containing "FAIL" simulates a provider
	 * outage.
	 */
	static class RecordingChatModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public synchronized ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			if (prompt.getUserMessage().getText().contains("FAIL")) {
				throw new IllegalStateException("simulated provider outage");
			}
			final ChatResponseMetadata metadata =
					ChatResponseMetadata.builder().model("stub-model").usage(new DefaultUsage(50, 5)).build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Reply " + prompts.size()))), metadata);
		}

		synchronized List<Prompt> prompts() {
			return List.copyOf(prompts);
		}

		synchronized void reset() {
			prompts.clear();
		}
	}
}