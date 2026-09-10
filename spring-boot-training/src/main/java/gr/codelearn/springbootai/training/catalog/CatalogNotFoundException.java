package gr.codelearn.springbootai.training.catalog;

/**
 * A referenced catalog row does not exist — an unknown customer email, product sku or order number. An unchecked exception, so a service
 * method that lets it escape rolls its transaction back under the <b>default</b> rule (Spring rolls back on {@code RuntimeException} /
 * {@code Error}, commits on checked exceptions).
 *
 * <p>Translated to an RFC 9457 {@code ProblemDetail} in one place in U4.2; for now it just surfaces to the test.
 */
class CatalogNotFoundException extends RuntimeException {

	CatalogNotFoundException(final String message) {
		super(message);
	}
}
