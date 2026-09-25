package gr.codelearn.springbootai.ai.config.web;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.common.InvalidModelOutputException;
import gr.codelearn.springbootai.ai.conversation.ConversationLimitException;
import gr.codelearn.springbootai.ai.conversation.ConversationNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * The single place a thrown exception becomes an HTTP response, as an RFC 9457 {@link ProblemDetail}. A trimmed-down version of
 * {@code spring-boot-training}'s {@code ApiExceptionHandler}.
 *
 * <p>{@link ResponseEntityExceptionHandler} already maps Spring MVC's own exceptions, including an unsupported or unparsable API version
 * (Spring Framework 7 API versioning), to a 400 {@link ProblemDetail}. On top of that:
 *
 * <ul>
 *   <li>a {@code @Valid} request body failure gets a machine-readable {@code errors} extension ({@code field} + {@code message});</li>
 *   <li>a failed model call becomes {@code 502 Bad Gateway}: the request was fine, the upstream LLM was not;</li>
 *   <li>so does unusable structured output (8.3), with the same {@code errors} extension naming the output's bad fields;</li>
 *   <li>an unknown conversation is {@code 404}, a full one (or a full store) {@code 409} (9.1).</li>
 * </ul>
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ChatModelUnavailableException.class)
	ProblemDetail handleChatModelUnavailable(final ChatModelUnavailableException ex) {
		// The cause carries the provider detail (connection refused, HTTP status); never the prompt itself.
		log.warn("Chat model call failed: {}", ex.getMessage(), ex.getCause());
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(ConversationNotFoundException.class)
	ProblemDetail handleConversationNotFound(final ConversationNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConversationLimitException.class)
	ProblemDetail handleConversationLimit(final ConversationLimitException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidModelOutputException.class)
	ProblemDetail handleInvalidModelOutput(final InvalidModelOutputException ex) {
		// Problems name fields and rules only; the model's raw output may echo user data and is not logged here.
		log.warn("Chat model output rejected: {} {}", ex.getMessage(), ex.getProblems());
		final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
		problemDetail.setProperty("errors", ex.getProblems()
											  .stream()
											  .map(problem -> Map.of("field", problem.field(), "message", problem.message()))
											  .toList());
		return problemDetail;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpHeaders headers,
																  final HttpStatusCode status, final WebRequest request) {
		final List<Map<String, String>> errors = ex.getBindingResult()
												   .getFieldErrors()
												   .stream()
												   .map(ApiExceptionHandler::toErrorEntry)
												   .toList();
		final ResponseEntity<Object> response = super.handleMethodArgumentNotValid(ex, headers, status, request);
		if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
			problemDetail.setProperty("errors", errors);
		}
		return response;
	}

	private static Map<String, String> toErrorEntry(final FieldError error) {
		return Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage()));
	}
}
