package gr.codelearn.springbootai.training.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A create request collides with an existing row on its business key — a product {@code sku} or a category {@code name} that is already
 * taken. Unchecked, so the service transaction rolls back.
 *
 * <p><b>U4.1 stopgap:</b> {@code @ResponseStatus(CONFLICT)} maps it straight to {@code 409} for now. U4.2 removes the annotation and
 * routes it through the central {@code @RestControllerAdvice} as an RFC 9457 {@code ProblemDetail}, next to
 * {@link CatalogNotFoundException}.
 */
@ResponseStatus(HttpStatus.CONFLICT)
class CatalogDuplicateException extends RuntimeException {

	CatalogDuplicateException(final String message) {
		super(message);
	}
}