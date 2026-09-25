package gr.codelearn.springbootai.ai.writing;

import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest;
import gr.codelearn.springbootai.ai.writing.dto.SummaryRequest;
import gr.codelearn.springbootai.ai.writing.dto.WritingResponse;

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
 * Unit 8.1: one endpoint per use case, each backed by its own prompt template. The client sends only the template's values, never prompt
 * text: {@code POST /api/v1/summaries} and {@code POST /api/v1/product-descriptions}. Stateless, like the chat endpoints; the generated
 * text is returned, not stored, hence {@code 200} rather than {@code 201}.
 *
 * <p>Versioned the same way as {@code chat.ChatController} (path segment 1, {@code version = "v1"}).
 */
@RestController
@RequestMapping(path = "/api/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "writing")
class WritingController {

	private final WritingService writingService;

	WritingController(final WritingService writingService) {
		this.writingService = writingService;
	}

	@PostMapping(path = "/summaries", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Summarize a text",
	           description = "Fills the summary template with the text, a word limit and an optional audience.")
	@ApiResponse(responseCode = "200", description = "The summary, the model name, and the token usage")
	@ApiResponse(responseCode = "400", description = "Blank or oversized text, a word limit outside 10-300, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	WritingResponse summarize(@Valid @RequestBody final SummaryRequest request) {
		return writingService.summarize(request);
	}

	@PostMapping(path = "/product-descriptions", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Write a catalog description for a product",
			   description = "Fills the product-description template with the product's facts, a tone and an optional language.")
	@ApiResponse(responseCode = "200", description = "The description, the model name, and the token usage")
	@ApiResponse(responseCode = "400",
				 description = "A blank name or category, no features or more than 10, an unknown tone, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	WritingResponse describeProduct(@Valid @RequestBody final ProductDescriptionRequest request) {
		return writingService.describeProduct(request);
	}
}