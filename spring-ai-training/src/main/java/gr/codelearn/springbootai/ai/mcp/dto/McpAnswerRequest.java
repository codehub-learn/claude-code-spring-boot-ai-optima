package gr.codelearn.springbootai.ai.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A question for a model that may use the tools of the connected MCP servers.
 *
 * @param question what to ask
 */
public record McpAnswerRequest(
		@Schema(example = "What time is it in Athens, and what is 19.99 plus 0.01?") @NotBlank @Size(max = 1000) String question) {
}
