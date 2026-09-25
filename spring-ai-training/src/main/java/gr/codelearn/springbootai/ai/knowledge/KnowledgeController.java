package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerRequest;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeIngestionResponse;

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
 * Unit 10.1: the knowledge base's two RAG steps as endpoints: {@code POST /knowledge/ingestions} (re)builds the vector store from the
 * sources, {@code POST /knowledge/answers} answers a question from it. Versioned like the other controllers (path segment 1).
 */
@RestController
@RequestMapping(path = "/api/{version}/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "knowledge")
class KnowledgeController {

	private final KnowledgeIngestionService ingestionService;
	private final KnowledgeService knowledgeService;

	KnowledgeController(final KnowledgeIngestionService ingestionService, final KnowledgeService knowledgeService) {
		this.ingestionService = ingestionService;
		this.knowledgeService = knowledgeService;
	}

	@PostMapping(path = "/ingestions", version = "v1")
	@Operation(summary = "Rebuild the knowledge base",
			   description = "Reads the catalog and the bundled documents, embeds them, and replaces their vectors in the store.")
	@ApiResponse(responseCode = "200", description = "How many documents of each source were ingested, and how long it took")
	@ApiResponse(responseCode = "502", description = "The embedding model or the vector store is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	KnowledgeIngestionResponse ingest() {
		return ingestionService.ingest();
	}

	@PostMapping(path = "/answers", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Answer a question from the knowledge base",
			   description = "Retrieves the closest documents, answers only from them, and returns them as sources.")
	@ApiResponse(responseCode = "200", description = "The answer, the retrieved sources, the model name, and the token usage")
	@ApiResponse(responseCode = "400", description = "A blank or oversized question, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat or embedding model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	KnowledgeAnswerResponse answer(@Valid @RequestBody final KnowledgeAnswerRequest request) {
		return knowledgeService.answer(request);
	}
}