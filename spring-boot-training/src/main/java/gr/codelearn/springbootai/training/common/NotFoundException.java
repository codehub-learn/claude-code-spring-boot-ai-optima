package gr.codelearn.springbootai.training.common;

/**
 * A referenced row does not exist — an unknown customer email, product sku or order number. Shared across the feature modules
 * ({@code catalog}, {@code order}). An unchecked exception, so a service method that lets it escape rolls its transaction back under the
 * <b>default</b> rule (Spring rolls back on {@code RuntimeException} / {@code Error}, commits on checked exceptions).
 *
 * <p>Mapped to a {@code 404} RFC 9457 {@code ProblemDetail} by {@code config.web.ApiExceptionHandler} (U4.2) — the one place every
 * exception in the API becomes an HTTP response. It carries no {@code @ResponseStatus} of its own on purpose: that U4.1 stopgap would
 * bypass the central advice and could drift out of sync with it.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(final String message) {
		super(message);
	}
}
