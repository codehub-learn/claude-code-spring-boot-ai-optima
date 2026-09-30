package gr.codelearn.springbootai.ai.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One tool a connected MCP server offers, as the model sees it.
 *
 * @param name        the tool's name
 * @param description what it does, in the words the server wrote for the model
 * @param inputSchema the JSON schema of its arguments
 */
public record McpToolResponse(
		@Schema(example = "currentTime") String name,
		@Schema(example = "Returns the current date and time in an IANA time zone") String description,
		@Schema(description = "JSON schema of the arguments") String inputSchema) {
}
