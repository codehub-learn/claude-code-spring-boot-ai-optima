package gr.codelearn.springbootai.ai.config.ai;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Unit 11.2: what the model reads when a tool rejects its arguments. Spring AI hands the model whatever
 * {@link ToolExecutionExceptionProcessor#process} returns for a failed call; the default is the exception's message, which for a
 * {@link ConstraintViolationException} is {@code getProduct.input.sku: must be letters, ...}: a method path first, the violations in no set
 * order, and nothing saying that the tool did not run. This processor says that, lists each wrong argument and why in a stable order, and
 * asks for a corrected call. Every other failure goes to Spring AI's default processor unchanged (Unit 11.3 designs those).
 *
 * <p>Spring AI's auto-configuration backs off for a {@link ToolExecutionExceptionProcessor} bean, so this one replaces its default for
 * every tool call in the application.
 */
@Component
class ToolValidationExceptionProcessor implements ToolExecutionExceptionProcessor {

	private final ToolExecutionExceptionProcessor fallback = DefaultToolExecutionExceptionProcessor.builder().build();

	@Override
	public String process(final ToolExecutionException exception) {
		if (rootCause(exception) instanceof ConstraintViolationException violations) {
			return "Invalid arguments, the tool did not run: " + describe(violations) + ". Fix the arguments and call the tool again.";
		}
		return fallback.process(exception);
	}

	private static Throwable rootCause(final Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && !(current instanceof ConstraintViolationException)) {
			current = current.getCause();
		}
		return current;
	}

	/**
	 * {@code sku must be letters...; maxPrice must be greater than 0...}: the last segment of each property path, sorted so the message is
	 * stable. A violation on the input itself (a missing object) has the parameter's name as its last segment.
	 */
	private static String describe(final ConstraintViolationException exception) {
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