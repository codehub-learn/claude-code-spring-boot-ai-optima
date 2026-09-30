package gr.codelearn.springbootai.ai.common;

/**
 * Unit 11.3: marks an exception whose message is written to be read, verbatim, by a model. When a tool throws one, the model receives its
 * message as the tool's result. Any other exception is unexpected: its message may hold SQL, hostnames or stack detail, so the model gets a
 * generic message and the details are logged (see {@code config.ai.ToolErrorProcessor}).
 *
 * <p>Implement it only where the message is safe to show and useful to act on: a domain fact ("No product with SKU X"), not a
 * technical failure.
 */
public interface ModelReadableException {

	/**
	 * The message, the same one {@link Throwable#getMessage()} returns.
	 */
	String getMessage();
}