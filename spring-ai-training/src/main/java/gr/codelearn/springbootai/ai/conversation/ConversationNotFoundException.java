package gr.codelearn.springbootai.ai.conversation;

import java.util.UUID;

/**
 * Unit 9.1: no conversation with this id: never created, deleted, or lost on restart (the store is in memory). Mapped to
 * {@code 404 Not Found} by {@code config.web.ApiExceptionHandler}.
 */
public class ConversationNotFoundException extends RuntimeException {

	ConversationNotFoundException(final UUID id) {
		super("No conversation with id " + id);
	}
}