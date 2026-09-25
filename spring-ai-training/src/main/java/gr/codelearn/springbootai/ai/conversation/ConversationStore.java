package gr.codelearn.springbootai.ai.conversation;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit 9.1: where the conversations live, deliberately the simplest thing that works: a map in this JVM's heap. So the state is lost on
 * restart and not shared between instances, and every conversation's history is sent in full on each turn. Unit 9.2 replaces this class
 * with Spring AI's {@code ChatMemory} (a message window, a repository behind it).
 *
 * <p>Heap is not free, so the store is bounded instead of silently growing: at most {@value #MAX_CONVERSATIONS} open conversations. Past
 * that, creating one fails until a client deletes one; there is no eviction here (a window and eviction are 9.2's topic).
 */
@Component
class ConversationStore {

	static final int MAX_CONVERSATIONS = 1_000;

	private final Map<UUID, Conversation> conversations = new ConcurrentHashMap<>();

	/**
	 * Synchronized so that the size check and the insert are one step; creating is rare and cheap, turns don't go through here.
	 */
	synchronized Conversation create() {
		if (conversations.size() >= MAX_CONVERSATIONS) {
			throw new ConversationLimitException("Too many open conversations (" + MAX_CONVERSATIONS + "), delete one first");
		}
		final Conversation conversation = new Conversation(UUID.randomUUID(), Instant.now());
		conversations.put(conversation.id(), conversation);
		return conversation;
	}

	Optional<Conversation> find(final UUID id) {
		return Optional.ofNullable(conversations.get(id));
	}

	boolean delete(final UUID id) {
		return conversations.remove(id) != null;
	}
}