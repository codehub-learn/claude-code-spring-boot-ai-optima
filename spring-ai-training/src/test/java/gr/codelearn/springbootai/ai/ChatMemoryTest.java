package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 9.2: how Spring AI 2.0.1's chat memory behaves, proven with the in-memory repository and a stub model (no database, no Spring
 * context). The application runs the same {@link MessageWindowChatMemory} and {@link MessageChatMemoryAdvisor} over the JDBC repository.
 *
 * <ul>
 *   <li>the window: eviction by count, a kept window that always starts at a user message, system messages never evicted;</li>
 *   <li>the advisor: what it adds to the prompt, what it stores and when, and that it needs a conversation id.</li>
 * </ul>
 */
class ChatMemoryTest {

	private static final String ID = "c-1";

	private final InMemoryChatMemoryRepository repository = new InMemoryChatMemoryRepository();

	@Test
	void theWindowDropsTheOldestMessages() {
		final ChatMemory memory = window(4);

		addTurns(memory, 3);

		assertThat(texts(memory.get(ID))).containsExactly("Q2", "A2", "Q3", "A3");
		assertThat(texts(repository.findByConversationId(ID))).as("evicted for good, not just hidden").hasSize(4);
	}

	@Test
	void theKeptWindowStartsAtAUserMessage() {
		// An odd window: cutting by count alone would keep A1 without Q1. The cut snaps forward to the next user message instead.
		final ChatMemory memory = window(3);

		addTurns(memory, 2);

		assertThat(texts(memory.get(ID))).containsExactly("Q2", "A2");
	}

	@Test
	void systemMessagesAreNeverEvicted() {
		final ChatMemory memory = window(3);
		memory.add(ID, new SystemMessage("Be brief."));

		addTurns(memory, 3);

		assertThat(texts(memory.get(ID))).containsExactly("Be brief.", "Q3", "A3");
	}

	@Test
	void theAdvisorPutsMemoryBeforeTheNewMessageAndStoresTheTurn() {
		final ChatMemory memory = window(20);
		final RecordingModel model = new RecordingModel();
		final ChatClient client = ChatClient.builder(model).defaultSystem("You are helpful.").build();

		ask(client, memory, "My name is Maria.");
		ask(client, memory, "What is my name?");

		assertThat(model.prompts.getLast().getInstructions()).extracting(Message::getMessageType)
															 .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT,
																			  MessageType.USER);
		// The system prompt is sent every time but never stored: it is the application's, not the conversation's.
		assertThat(texts(memory.get(ID))).containsExactly("My name is Maria.", "Reply 1", "What is my name?", "Reply 2");
	}

	@Test
	void pitfallTheAdvisorStoresTheUserMessageBeforeTheCall() {
		final ChatMemory memory = window(20);
		final ChatClient client = ChatClient.builder(prompt -> {
			throw new IllegalStateException("provider down");
		}).build();

		assertThatThrownBy(() -> ask(client, memory, "Hello?")).hasMessage("provider down");

		// Half a turn: a question without an answer. ConversationService writes the previous window back when this happens.
		assertThat(memory.get(ID)).extracting(Message::getMessageType).containsExactly(MessageType.USER);
	}

	@Test
	void theAdvisorNeedsAConversationId() {
		final ChatClient client = ChatClient.builder(new RecordingModel()).build();
		final MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(window(20)).build();

		// Why it is not a default advisor of the central client: the stateless endpoints have no id to give it.
		assertThatThrownBy(() -> client.prompt().user("Hi").advisors(advisor).call().content()).hasMessageContaining(
				"conversationId cannot be null");
	}

	private ChatMemory window(final int maxMessages) {
		return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(maxMessages).build();
	}

	private static void addTurns(final ChatMemory memory, final int turns) {
		for (int i = 1; i <= turns; i++) {
			memory.add(ID, List.of(new UserMessage("Q" + i), new AssistantMessage("A" + i)));
		}
	}

	private static void ask(final ChatClient client, final ChatMemory memory, final String question) {
		client.prompt()
			  .user(question)
			  .advisors(
				      advisors -> advisors.advisors(MessageChatMemoryAdvisor.builder(memory).build()).param(ChatMemory.CONVERSATION_ID,
																											ID))
			  .call()
			  .content();
	}

	private static List<String> texts(final List<Message> messages) {
		return messages.stream().map(Message::getText).toList();
	}

	private static final class RecordingModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Reply " + prompts.size()))));
		}
	}
}