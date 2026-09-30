package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerRequest;
import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerResponse;
import gr.codelearn.springbootai.ai.mcp.dto.McpToolResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Unit 11.4: the MCP client's two endpoints, {@code GET /mcp/tools} (what the connected server offers) and {@code POST /mcp/answers} (a
 * question answered with those tools). Versioned like the other controllers (path segment 1).
 */
@RestController
@RequestMapping(path = "/api/{version}/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "mcp")
class McpController {

	private final McpService mcpService;

	McpController(final McpService mcpService) {
		this.mcpService = mcpService;
	}

	@GetMapping(path = "/tools", version = "v1")
	@Operation(summary = "List the tools of the connected MCP servers",
			   description = "Asks the MCP server (tools/list over HTTP) what it offers: the same name, description and argument schema " +
							 "the model is given.")
	@ApiResponse(responseCode = "200", description = "The tools, possibly none")
	@ApiResponse(responseCode = "502", description = "The MCP server is unreachable, or no MCP client is configured",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	List<McpToolResponse> tools() {
		return mcpService.listTools();
	}

	@PostMapping(path = "/answers", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Ask a question that the MCP server's tools can help with",
			   description = "The model may call the tools of the connected MCP servers before answering. The response lists every tool " +
							 "call it made.")
	@ApiResponse(responseCode = "200", description = "The answer, the tool calls, the model name, and the token usage of all rounds")
	@ApiResponse(responseCode = "400", description = "A blank or oversized question, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The MCP server or the chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	McpAnswerResponse answer(@Valid @RequestBody final McpAnswerRequest request) {
		return mcpService.answer(request);
	}
}