package gr.codelearn.springbootai.training.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A referenced catalog row does not exist — an unknown customer email, product sku or order number. An unchecked exception, so a service
 * method that lets it escape rolls its transaction back under the <b>default</b> rule (Spring rolls back on {@code RuntimeException} /
 * {@code Error}, commits on checked exceptions).
 *
 * <p><b>U4.1 stopgap:</b> {@code @ResponseStatus(NOT_FOUND)} maps it straight to {@code 404} so the controllers have a working failure
 * path to test. U4.2 removes the annotation and translates this exception to an RFC 9457 {@code ProblemDetail} in one place, a central
 * {@code @RestControllerAdvice}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
class CatalogNotFoundException extends RuntimeException {

	CatalogNotFoundException(final String message) {
		super(message);
	}
}