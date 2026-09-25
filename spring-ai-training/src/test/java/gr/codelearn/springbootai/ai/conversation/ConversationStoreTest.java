package gr.codelearn.springbootai.ai.conversation;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 9.1: the in-memory store is bounded, and deleting frees a place.
 */
class ConversationStoreTest {

	private final ConversationStore store = new ConversationStore();

	@Test
	void createFindAndDelete() {
		final Conversation conversation = store.create();

		assertThat(store.find(conversation.id())).containsSame(conversation);
		assertThat(store.delete(conversation.id())).isTrue();
		assertThat(store.find(conversation.id())).isEmpty();
		assertThat(store.delete(conversation.id())).isFalse();
		assertThat(store.find(UUID.randomUUID())).isEmpty();
	}

	@Test
	void theStoreIsBoundedUntilOneIsDeleted() {
		final UUID first = store.create().id();
		IntStream.range(1, ConversationStore.MAX_CONVERSATIONS).forEach(i -> store.create());

		assertThatThrownBy(store::create).isInstanceOf(ConversationLimitException.class).hasMessageContaining(
				"Too many open conversations");

		store.delete(first);
		assertThat(store.create()).isNotNull();
	}
}