package gr.codelearn.springbootai.ai.mcp;

import org.jspecify.annotations.Nullable;

/**
 * The MCP server could not be reached, or no MCP client is configured. Mapped to {@code 502 Bad Gateway} by
 * {@code config.web.ApiExceptionHandler}. The cause (connection refused, timeout) goes to the log, not to the caller.
 */
public class McpServerUnavailableException extends RuntimeException {

	public McpServerUnavailableException(final String message, final @Nullable Throwable cause) {
		super(message, cause);
	}
}
