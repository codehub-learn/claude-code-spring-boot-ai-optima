package gr.codelearn.springbootai.ai.conversation;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Unit 9.2: a conversation's registry row (table {@code conversation}), a Spring Data JDBC aggregate. It says that the conversation exists
 * and how many turns it has had; the messages themselves are in Spring AI's chat memory table, keyed by the same id.
 *
 * <p>The id is assigned here, not by the database, so Spring Data JDBC tells a new row from an existing one by {@link #version()}:
 * {@code null} means insert. The version also makes a lost update fail instead of silently overwriting {@link #turns()}.
 *
 * @param id        the conversation id, a UUID as text (Oracle has no UUID type; the memory table keys on the same text)
 * @param createdAt when it was started
 * @param turns     completed turns since it started, including those the memory window has since dropped
 * @param version   optimistic-locking version, {@code null} before the first save
 */
record Conversation(@Id String id, Instant createdAt, int turns, @Version @Nullable Long version) {

	static Conversation start(final UUID id, final Instant now) {
		return new Conversation(id.toString(), now, 0, null);
	}

	Conversation withNextTurn() {
		return new Conversation(id, createdAt, turns + 1, version);
	}
}