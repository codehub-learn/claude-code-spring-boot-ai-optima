package gr.codelearn.springbootai.training.config.web;

import gr.codelearn.springbootai.training.catalog.internal.CatalogDuplicateException;
import gr.codelearn.springbootai.training.common.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * The single place a thrown exception becomes an HTTP response (U4.2), as RFC 9457 {@link ProblemDetail} — CLAUDE.md's "Central
 * {@code @RestControllerAdvice}. No custom ad-hoc error JSON." Replaces the two {@code @ResponseStatus} stopgaps U4.1 left on
 * {@link NotFoundException} and {@link CatalogDuplicateException}.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} to reuse Spring MVC's own {@link ProblemDetail} handling for the framework
 * exceptions (bad JSON, wrong HTTP method, unsupported {@code Accept}, …) and only overrides the two Bean Validation cases to attach a
 * machine-readable {@code errors} extension:
 *
 * <ul>
 *   <li>a {@code @Valid} request body failure throws {@link MethodArgumentNotValidException} — <b>unless</b> the same method also has a
 *       directly-constrained parameter (see the next bullet), in which case it throws the type below instead.</li>
 *   <li>a {@code @Validated} path-variable / request-param failure throws {@link HandlerMethodValidationException} — <b>not</b>
 *       {@code ConstraintViolationException} (that type is what {@code jakarta.validation.Validator} throws directly; Spring MVC 6.1+
 *       wraps controller-method parameter violations in its own type instead, a common trap when porting older examples). U4.3 found
 *       that a method combining this with a cascaded {@code @Valid} body (e.g. {@code patchProduct}) reports the body's failures
 *       through this same exception too — {@link #handleHandlerMethodValidationException} unwraps both shapes.</li>
 * </ul>
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(final NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(CatalogDuplicateException.class)
	ProblemDetail handleDuplicate(final CatalogDuplicateException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpHeaders headers,
																  final HttpStatusCode status, final WebRequest request) {
		final List<Map<String, String>> errors = ex.getBindingResult()
												   .getFieldErrors()
												   .stream()
												   .map(ApiExceptionHandler::toErrorEntry)
												   .toList();
		return withErrors(super.handleMethodArgumentNotValid(ex, headers, status, request), errors);
	}

	/**
	 * A method mixing a directly-constrained parameter (a {@code @NotBlank @PathVariable}, say) with a cascaded {@code @Valid}/
	 * {@code @Validated} {@code @RequestBody} — every endpoint here that takes both, e.g. {@code patchProduct} — routes <b>both</b> kinds
	 * of failure through this one exception, not just the path/query one. Each
	 * {@link org.springframework.validation.method .ParameterValidationResult} is either a plain scalar violation (no per-field path — the
	 * parameter name itself, e.g. {@code "sku"}, is the most specific field there is) or a {@link ParameterErrors} — the subtype Spring
	 * uses for a cascaded object argument, which is itself a Spring {@code Errors} and carries real {@link FieldError}s ({@code "price"},
	 * not the parameter name {@code "request"}). Caught by {@code CatalogControllerTest.patchProductWithANonPositivePriceReturns400}
	 * expecting {@code price} and getting {@code request} before this handled the two cases separately.
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(final HandlerMethodValidationException ex,
																			final HttpHeaders headers, final HttpStatusCode status,
																			final WebRequest request) {
		final List<Map<String, String>> errors = ex.getParameterValidationResults()
												   .stream()
												   .flatMap(result -> result instanceof final ParameterErrors cascaded
																	  ? cascaded.getFieldErrors()
		                                                                        .stream()
		                                                                        .map(ApiExceptionHandler::toErrorEntry)
																	  : result.getResolvableErrors()
																			  .stream()
																			  .map(error -> Map.of(
																					  "field",
				                                                                      result.getMethodParameter().getParameterName(),
																					  "message",
				                                                                      String.valueOf(error.getDefaultMessage()))))
												   .toList();
		return withErrors(super.handleHandlerMethodValidationException(ex, headers, status, request), errors);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(final Exception ex) {
		log.error("Unhandled exception reaching the API boundary", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private static Map<String, String> toErrorEntry(final FieldError error) {
		return Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage()));
	}

	private static ResponseEntity<Object> withErrors(final ResponseEntity<Object> response, final List<Map<String, String>> errors) {
		if (response.getBody() instanceof final ProblemDetail problemDetail) {
			problemDetail.setProperty("errors", errors);
		}
		return response;
	}
}