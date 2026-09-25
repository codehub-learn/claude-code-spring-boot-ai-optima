package gr.codelearn.springbootai.ai.extraction;

import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest;
import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionResponse;

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
 * Unit 8.3: {@code POST /api/v1/product-extractions}. Free text in, a typed and validated product out. Nothing is stored, hence {@code 200}
 * rather than {@code 201}.
 *
 * <p>Versioned the same way as {@code chat.ChatController} (path segment 1, {@code version = "v1"}).
 */
@RestController
@RequestMapping(path = "/api/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "extraction")
class ExtractionController {

	private final ExtractionService extractionService;

	ExtractionController(final ExtractionService extractionService) {
		this.extractionService = extractionService;
	}

	@PostMapping(path = "/product-extractions", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Extract a product listing from free text",
			   description = "The model's JSON is checked against the record's schema (re-prompting on errors), mapped, and validated.")
	@ApiResponse(responseCode = "200", description = "The product, the output mode, the model name, and the token usage")
	@ApiResponse(responseCode = "400", description = "Blank or oversized text, an unknown output mode, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model failed, or its output stayed invalid (with an errors list)",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ProductExtractionResponse extract(@Valid @RequestBody final ProductExtractionRequest request) {
		return extractionService.extract(request);
	}
}