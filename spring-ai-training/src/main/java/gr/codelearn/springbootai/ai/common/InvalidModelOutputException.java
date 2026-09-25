package gr.codelearn.springbootai.ai.common;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The chat model answered, but its structured output is unusable: not JSON for the requested schema, or valid JSON that breaks a business
 * rule (Jakarta Bean Validation on the mapped record). Unit 8.3. Mapped to {@code 502 Bad Gateway} by
 * {@code config.web.ApiExceptionHandler}, with the problems in an {@code errors} extension: the request was fine, the upstream model's
 * output was not.
 */
public class InvalidModelOutputException extends RuntimeException {

	private final List<Problem> problems;

	public InvalidModelOutputException(final String message, final List<Problem> problems, final @Nullable Throwable cause) {
		super(message, cause);
		this.problems = List.copyOf(problems);
	}

	public List<Problem> getProblems() {
		return problems;
	}

	/**
	 * One thing wrong with the output.
	 *
	 * @param field   the property path in the output ({@code $} for the output as a whole)
	 * @param message what is wrong with it
	 */
	public record Problem(String field, String message) {
	}
}