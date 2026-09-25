package gr.codelearn.springbootai.ai.common;

import org.jspecify.annotations.Nullable;

/**
 * The chat model could not produce an answer: the provider was unreachable, returned an error, or retries ran out. Thrown by every feature
 * service that calls the model ({@code chat}, {@code writing}), mapped to {@code 502 Bad Gateway} by
 * {@code config.web.ApiExceptionHandler}. Shared kernel since 8.1 (it lived in {@code chat.internal} while chat was the only caller).
 */
public class ChatModelUnavailableException extends RuntimeException {

	public ChatModelUnavailableException(final String message, final @Nullable Throwable cause) {
		super(message, cause);
	}
}