package gr.codelearn.springbootai.persistencedemo.common;

/**
 * Thrown by the services when a lookup by business key finds nothing. The web layer (a later step) translates it into a 404
 * {@code ProblemDetail}.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(final String entity, final String key) {
		super("%s '%s' not found".formatted(entity, key));
	}
}