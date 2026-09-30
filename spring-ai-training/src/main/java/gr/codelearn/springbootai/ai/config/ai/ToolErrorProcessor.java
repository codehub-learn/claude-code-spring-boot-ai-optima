package gr.codelearn.springbootai.ai.config.ai;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ModelReadableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Units 11.2 and 11.3: the one place where a failed tool call becomes the text a model reads. Spring AI hands the model whatever
 * {@link ToolExecutionExceptionProcessor#process} returns; its default is the exception's own message, which is fine for "No product with
 * SKU X" and dangerous for anything else (a SQL error, a hostname, a stack detail). Here the exception's <em>kind</em> decides, looking
 * through the {@link Throwable#getCause() causes} Spring AI wraps it in:
 *
 * <table>
 *   <caption>What the model reads</caption>
 *   <tr><th>Failure</th><th>Message</th></tr>
 *   <tr><td>{@link ConstraintViolationException} (11.2)</td><td>the tool did not run, which arguments are wrong and why, call
 *   again</td></tr>
 *   <tr><td>{@link ExternalServiceException} (11.3)</td><td>what failed, then what to do about that {@code Kind} (retry or not, what to
 *   tell the customer)</td></tr>
 *   <tr><td>arguments that are not JSON of the tool's shape (Jackson)</td><td>the tool did not run, send the schema's shape, call
 *   again; Jackson's own message quotes the payload and Java class names</td></tr>
 *   <tr><td>any {@link ModelReadableException}</td><td>its message, as it is</td></tr>
 *   <tr><td>anything else</td><td>a generic message that says not to guess; the exception is logged, not shown</td></tr>
 * </table>
 *
 * <p>Spring AI's auto-configuration backs off for a {@link ToolExecutionExceptionProcessor} bean, so this one replaces its default for
 * every tool call in the application. {@link #describe} is the same text without the logging: {@code RecordingToolCallback} puts it in the
 * response, so the {@code error} a caller sees is exactly what the model was told, and an unexpected exception's message reaches
 * neither.
 */
@Component
public class ToolErrorProcessor implements ToolExecutionExceptionProcessor {

	public static final String UNEXPECTED_MESSAGE =
			"The tool failed unexpectedly and gave no result. Do not guess one: tell the customer you could not "
			+ "complete this part of the request.";

	static final String MALFORMED = "The arguments could not be read, the tool did not run: send one JSON object with the fields in the "
									+ "tool's input schema, with the right types. Fix the arguments and call the tool again.";

	private static final Logger log = LoggerFactory.getLogger(ToolErrorProcessor.class);

	@Override
	public String process(final ToolExecutionException exception) {
		final Optional<String> known = describeKnown(exception);
		final ExternalServiceException external = find(exception, ExternalServiceException.class);
		if (known.isEmpty()) {
			// The full exception, for whoever operates this; the model never sees it.
			log.error("Tool {} failed unexpectedly", exception.getToolDefinition().name(), exception);
		} else if (external != null) {
			log.warn("Tool {} could not use an external service ({}): {}", exception.getToolDefinition().name(), external.kind(),
					 external.getMessage());
		}
		return known.orElse(UNEXPECTED_MESSAGE);
	}

	/**
	 * What a model is told when {@code failure} ended a tool call. Pure: no logging, so it can be called for the response as well.
	 */
	public String describe(final Throwable failure) {
		return describeKnown(failure).orElse(UNEXPECTED_MESSAGE);
	}

	private static Optional<String> describeKnown(final Throwable failure) {
		final ConstraintViolationException violations = find(failure, ConstraintViolationException.class);
		if (violations != null) {
			return Optional.of("Invalid arguments, the tool did not run: " + violationList(violations)
							   + ". Fix the arguments and call the tool again.");
		}
		final ExternalServiceException external = find(failure, ExternalServiceException.class);
		if (external != null) {
			return Optional.of(external.getMessage() + ". " + external.kind().advice());
		}
		if (find(failure, JacksonException.class) != null) {
			// The arguments were not JSON, or not the shape the tool takes. Jackson's message quotes the payload and Java class names.
			return Optional.of(MALFORMED);
		}
		final ModelReadableException readable = find(failure, ModelReadableException.class);
		return readable == null ? Optional.empty() : Optional.of(readable.getMessage());
	}

	/**
	 * The first throwable of {@code type} in the cause chain, starting with {@code throwable} itself; {@code null} when there is none.
	 */
	private static <T> @Nullable T find(final Throwable throwable, final Class<T> type) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (type.isInstance(current)) {
				return type.cast(current);
			}
		}
		return null;
	}

	/**
	 * {@code sku must be letters...; maxPrice must be greater than 0...}: the last segment of each property path, sorted so the message is
	 * stable. A violation on the input itself (a missing object) has the parameter's name as its last segment.
	 */
	private static String violationList(final ConstraintViolationException exception) {
		return exception.getConstraintViolations()
						.stream()
						.sorted(Comparator.comparing((ConstraintViolation<?> violation) -> violation.getPropertyPath().toString()))
						.map(violation -> lastSegment(violation.getPropertyPath().toString()) + " " + violation.getMessage())
						.collect(Collectors.joining("; "));
	}

	private static String lastSegment(final String path) {
		return path.substring(path.lastIndexOf('.') + 1);
	}
}