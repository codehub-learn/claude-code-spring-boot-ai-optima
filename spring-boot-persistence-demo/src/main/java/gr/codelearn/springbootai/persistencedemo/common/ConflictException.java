package gr.codelearn.springbootai.persistencedemo.common;

/**
 * Thrown by the services when a request conflicts with the current state: a business key that already exists, or a transition the
 * domain does not allow. The web layer (a later step) translates it into a 409 {@code ProblemDetail}.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(final String message) {
		super(message);
	}
}