package gr.codelearn.springbootai.ai.common;

import org.jspecify.annotations.Nullable;

/**
 * Unit 11.3: a service outside this application failed to give an answer, classified by what the caller should do about it. Thrown by the
 * client of the external service (after its own retries) with a factual message that is safe for a model to read: no URLs, status codes or
 * stack text, which go to the log through the cause.
 */
public class ExternalServiceException extends RuntimeException implements ModelReadableException {

	/**
	 * What went wrong, in terms of what to do next. {@link #advice()} is what a model is told to do about it.
	 */
	public enum Kind {

		/**
		 * Down, erroring or too slow: it may work later, and the client already retried.
		 */
		UNAVAILABLE("Do not retry and do not guess an answer. Tell the customer this is not available right now and to try again later."),

		/**
		 * Asked too often: it will work again after a wait.
		 */
		RATE_LIMITED("Do not retry now. Tell the customer to ask again in a few minutes."),

		/**
		 * Refused this input: asking again the same way gives the same answer.
		 */
		REJECTED("Retrying with the same input will not help. Tell the customer, and offer to try different input.");

		private final String advice;

		Kind(final String advice) {
			this.advice = advice;
		}

		public String advice() {
			return advice;
		}
	}

	private final Kind kind;

	public ExternalServiceException(final Kind kind, final String message, final @Nullable Throwable cause) {
		super(message, cause);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}
}