package gr.codelearn.springbootai.ai.conversation;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import javax.sql.DataSource;
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
 *
 * <p>Unit 9.2: the same API on Spring AI's chat memory in the Testcontainers Oracle, with the default window of 20 messages. Adds the
 * window's eviction, the rollback of a failed turn, memory that outlives the {@code ChatMemory} that wrote it, and the cascade on delete.
 */
@AutoConfigureMockMvc
class ConversationApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RecordingChatModel chatModel;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcClient jdbcClient;

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
		send(messages, "My name is Maria.").hasStatusOk();

		// The memory advisor saved "FAIL please" before the call; the service writes the previous window back.
		send(messages, "FAIL please").hasStatus(HttpStatus.BAD_GATEWAY);
		send(messages, "What is my name?").hasStatusOk().bodyJson().extractingPath("$.turn").convertTo(Integer.class).isEqualTo(2);

		// System prompt, then the first turn and the new question: no trace of the failed one.
		assertThat(texts(chatModel.prompts().getLast().getInstructions())).hasSize(4)
																		  .endsWith("My name is Maria.", "Reply 1", "What is my name?");
	}

	@Test
	void anEmptyReplyCannotBeStoredOnOracle() {
		final String messages = create() + "/messages";

		// Oracle stores '' as NULL and the memory table's content is NOT NULL: saving the reply fails, the turn is rolled back.
		send(messages, "EMPTY reply please").hasStatus(HttpStatus.BAD_GATEWAY);

		assertThat(mvc.get().uri(messages)).hasStatusOk().bodyJson().isEqualTo("[]");
	}

	@Test
	void theWindowKeepsTheLastTwentyMessages() {
		final String messages = create() + "/messages";
		IntStream.rangeClosed(1, 11).forEach(i -> send(messages, "Turn " + i).hasStatusOk());

		send(messages, "Turn 12").hasStatusOk().bodyJson().extractingPath("$.turn").convertTo(Integer.class).isEqualTo(12);

		// Turn 12 was sent with turns 2-11 (turn 1 had left the window), and after it memory holds turns 3-12.
		final List<String> lastPrompt = texts(chatModel.prompts().getLast().getInstructions());
		assertThat(lastPrompt).hasSize(1 + 20 + 1).doesNotContain("Turn 1").contains("Turn 2", "Turn 11").endsWith("Turn 12");
		final List<String> remembered = texts(chatMemory().get(id(messages)));
		assertThat(remembered).hasSize(20).startsWith("Turn 3").endsWith("Turn 12", "Reply 12");
	}

	@Test
	void memoryOutlivesTheChatMemoryThatWroteIt() {
		final String messages = create() + "/messages";
		send(messages, "My name is Maria.").hasStatusOk();

		// A new repository and window over the same database: what a restarted application would build.
		assertThat(texts(chatMemory().get(id(messages)))).containsExactly("My name is Maria.", "Reply 1");
	}

	@Test
	void deletingAConversationDeletesItsMemory() {
		final String conversation = create();
		send(conversation + "/messages", "My name is Maria.").hasStatusOk();

		assertThat(mvc.delete().uri(conversation)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(jdbcClient.sql("select count(*) from spring_ai_chat_memory where conversation_id = ?")
							 .param(id(conversation))
							 .query(Integer.class)
							 .single()).isZero();
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

	private ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder().chatMemoryRepository(JdbcChatMemoryRepository.builder().dataSource(dataSource).build())
									  .build();
	}

	private static String id(final String path) {
		final String conversation = path.endsWith("/messages") ? path.substring(0, path.length() - "/messages".length()) : path;
		return conversation.substring(conversation.lastIndexOf('/') + 1);
	}

	private static List<String> texts(final List<Message> messages) {
		return messages.stream().map(Message::getText).toList();
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
	 * outage, one containing "EMPTY" gets a reply with no text.
	 */
	static class RecordingChatModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public synchronized ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			if (prompt.getUserMessage().getText().contains("FAIL")) {
				throw new IllegalStateException("simulated provider outage");
			}
			final String text = prompt.getUserMessage().getText().contains("EMPTY") ? "" : "Reply " + prompts.size();
			final ChatResponseMetadata metadata =
					ChatResponseMetadata.builder().model("stub-model").usage(new DefaultUsage(50, 5)).build();
			return new ChatResponse(List.of(new Generation(new AssistantMessage(text))), metadata);
		}

		synchronized List<Prompt> prompts() {
			return List.copyOf(prompts);
		}

		synchronized void reset() {
			prompts.clear();
		}
	}
}