package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit 11.1: {@code POST /assistant/answers}, a question answered by a model that may call the catalog tools. Versioned like the other
 * controllers (path segment 1).
 */
@RestController
@RequestMapping(path = "/api/{version}/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "assistant")
class AssistantController {

	private final AssistantService assistantService;

	AssistantController(final AssistantService assistantService) {
		this.assistantService = assistantService;
	}

	@PostMapping(path = "/answers", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Ask the shop assistant",
			   description = "The model may call the catalog tools (search products, get a product by SKU) before answering. The "
							 + "response lists every tool call it made.")
	@ApiResponse(responseCode = "200", description = "The answer, the tool calls, the model name, and the token usage of all rounds")
	@ApiResponse(responseCode = "400", description = "A blank or oversized question, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	AssistantResponse answer(@Valid @RequestBody final AssistantRequest request) {
		return assistantService.answer(request);
	}
}