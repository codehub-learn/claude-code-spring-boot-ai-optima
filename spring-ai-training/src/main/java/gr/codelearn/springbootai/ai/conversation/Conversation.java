package gr.codelearn.springbootai.ai.conversation;

import org.springframework.ai.chat.messages.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unit 9.1: one server-side conversation, the state a stateless endpoint doesn't have. It holds the turns so far as Spring AI
 * {@link Message}s (user, assistant, user, ...), exactly what goes back to the model on the next turn.
 *
 * <p>A turn is taken under the conversation's own lock, model call included: two requests for the same conversation run one after the
 * other, so neither answers from a history that is missing the other's turn. Other conversations are not blocked. A {@link ReentrantLock},
 * not {@code synchronized}, so a waiting virtual thread doesn't pin its carrier.
 */
final class Conversation {

	private final UUID id;
	private final Instant createdAt;
	private final List<Message> messages = new ArrayList<>();
	private final ReentrantLock lock = new ReentrantLock();

	Conversation(final UUID id, final Instant createdAt) {
		this.id = id;
		this.createdAt = createdAt;
	}

	UUID id() {
		return id;
	}

	Instant createdAt() {
		return createdAt;
	}

	ReentrantLock lock() {
		return lock;
	}

	/**
	 * A copy of the history; callers read it while holding {@link #lock()} or accept a snapshot.
	 */
	List<Message> messages() {
		lock.lock();
		try {
			return List.copyOf(messages);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Completed turns: one user message plus one assistant reply each.
	 */
	int turns() {
		return messages().size() / 2;
	}

	/**
	 * Adds a finished turn. Only called with {@link #lock()} held and only after the model answered, so a failed call leaves no half turn
	 * behind.
	 */
	void addTurn(final Message user, final Message assistant) {
		messages.add(user);
		messages.add(assistant);
	}
}