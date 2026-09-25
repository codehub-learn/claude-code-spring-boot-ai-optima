package gr.codelearn.springbootai.ai.conversation;

/**
 * Unit 9.1: a bound of the in-memory state was reached: too many open conversations, or too many turns in one. Mapped to
 * {@code 409 Conflict} by {@code config.web.ApiExceptionHandler}: the request is fine, the server-side state doesn't allow it now.
 */
public class ConversationLimitException extends RuntimeException {

	ConversationLimitException(final String message) {
		super(message);
	}
}