package gr.codelearn.springbootai.ai.conversation;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageRequest;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse.Role;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse.TokenUsage;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationResponse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * Unit 9.2: the 9.1 conversation API, with Spring AI's chat memory instead of a hand-rolled store. A turn is now:
 *
 * <ol>
 *   <li>check the conversation exists (the {@link ConversationRepository} registry);</li>
 *   <li>call the model with the system prompt and the new message, plus the {@link MessageChatMemoryAdvisor} and the conversation id.
 *   The advisor loads the remembered messages, puts them before the new one, saves the new user message, and saves the reply after
 *   the call. This service never passes the history itself;</li>
 *   <li>count the turn in the registry.</li>
 * </ol>
 *
 * <p>Two differences from 9.1 that matter:
 *
 * <ul>
 *   <li><b>Memory is a window, not a transcript.</b> Only the last {@code app.ai.memory.max-messages} messages are kept (and sent); older
 *   ones are deleted. The prompt stops growing, so 9.1's hard turn limit is gone, but what fell out of the window is forgotten, by the
 *   model and by {@code GET .../messages} alike.</li>
 *   <li><b>A failed call would leave half a turn.</b> The advisor saves the user message <em>before</em> the call. If the model fails
 *   (or the reply cannot be stored), the window as it was before the turn is written back, so the next turn doesn't see a question
 *   without an answer.</li>
 * </ul>
 *
 * <p>Turns on one conversation still run one at a time: a {@link ReentrantLock} per conversation, picked from a fixed set of
 * {@value #LOCK_STRIPES} by the id's hash (bounded memory, no cleanup; two conversations occasionally share a lock and wait for each
 * other). The lock is in this JVM only: two instances of the app could interleave turns on the same conversation. The memory itself is in
 * the database, so it survives a restart and is visible to every instance. No {@code @Transactional}: each repository call commits on its
 * own, and no connection is held while the model answers.
 */
@Service
class ConversationService {

	static final int LOCK_STRIPES = 64;

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;
	private final MessageChatMemoryAdvisor chatMemoryAdvisor;
	private final ChatMemory chatMemory;
	private final ChatMemoryRepository chatMemoryRepository;
	private final ConversationRepository conversationRepository;
	private final List<ReentrantLock> locks = IntStream.range(0, LOCK_STRIPES).mapToObj(i -> new ReentrantLock()).toList();

	ConversationService(final ChatClient chatClient, final MessageChatMemoryAdvisor chatMemoryAdvisor, final ChatMemory chatMemory,
						final ChatMemoryRepository chatMemoryRepository, final ConversationRepository conversationRepository) {
		this.chatClient = chatClient;
		this.chatMemoryAdvisor = chatMemoryAdvisor;
		this.chatMemory = chatMemory;
		this.chatMemoryRepository = chatMemoryRepository;
		this.conversationRepository = conversationRepository;
	}

	ConversationResponse create() {
		final Conversation conversation = conversationRepository.save(Conversation.start(UUID.randomUUID(), Instant.now()));
		return new ConversationResponse(UUID.fromString(conversation.id()), conversation.createdAt());
	}

	ConversationReplyResponse send(final UUID id, final ConversationMessageRequest request) {
		final ReentrantLock lock = lockFor(id);
		lock.lock();
		try {
			final Conversation conversation = find(id);
			final ChatResponse response = call(conversation.id(), request.content());
			final Conversation counted = conversationRepository.save(conversation.withNextTurn());
			return toReply(id, counted.turns(), response);
		} finally {
			lock.unlock();
		}
	}

	List<ConversationMessageResponse> history(final UUID id) {
		final Conversation conversation = find(id);
		return chatMemory.get(conversation.id()).stream().map(ConversationService::toMessageResponse).toList();
	}

	void delete(final UUID id) {
		final ReentrantLock lock = lockFor(id);
		lock.lock();
		try {
			final Conversation conversation = find(id);
			// The memory rows go with it (on delete cascade); no separate chatMemory.clear(...) needed.
			conversationRepository.delete(conversation);
		} finally {
			lock.unlock();
		}
	}

	private Conversation find(final UUID id) {
		return conversationRepository.findById(id.toString()).orElseThrow(() -> new ConversationNotFoundException(id));
	}

	private ReentrantLock lockFor(final UUID id) {
		return locks.get(Math.floorMod(id.hashCode(), LOCK_STRIPES));
	}

	private ChatResponse call(final String conversationId, final String content) {
		final List<Message> before = chatMemory.get(conversationId);
		final ChatResponse response;
		try {
			response = chatClient.prompt()
								 .system(SystemPromptParams::fill)
								 .user(content)
								 .advisors(
					                     advisors -> advisors.advisors(chatMemoryAdvisor).param(ChatMemory.CONVERSATION_ID,
																								conversationId))
								 .call()
								 .chatResponse();
		} catch (RuntimeException ex) {
			chatMemoryRepository.saveAll(conversationId, before);
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			chatMemoryRepository.saveAll(conversationId, before);
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		return response;
	}

	private static ConversationReplyResponse toReply(final UUID id, final int turn, final ChatResponse response) {
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new ConversationReplyResponse(id, turn, Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
											 metadata.getModel(),
											 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	private static ConversationMessageResponse toMessageResponse(final Message message) {
		final Role role = message instanceof AssistantMessage ? Role.ASSISTANT : Role.USER;
		return new ConversationMessageResponse(role, Objects.requireNonNullElse(message.getText(), ""));
	}
}