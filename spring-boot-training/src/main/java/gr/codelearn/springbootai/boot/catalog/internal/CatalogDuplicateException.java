package gr.codelearn.springbootai.boot.catalog.internal;

/**
 * A create request collides with an existing row on its business key — a product {@code sku} or a category {@code name} that is already
 * taken. Unchecked, so the service transaction rolls back.
 *
 * <p>Mapped to a {@code 409} RFC 9457 {@code ProblemDetail} by {@code config.web.ApiExceptionHandler} (U4.2), next to
 * {@link NotFoundException}. No {@code @ResponseStatus} here either — see that class's Javadoc for why.
 */
public class CatalogDuplicateException extends RuntimeException {

	public CatalogDuplicateException(final String message) {
		super(message);
	}
}