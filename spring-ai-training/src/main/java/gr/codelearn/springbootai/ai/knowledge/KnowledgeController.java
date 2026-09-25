package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerRequest;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeIngestionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unit 10.1: the knowledge base's two RAG steps as endpoints: {@code POST /knowledge/ingestions} (re)builds the vector store from the
 * sources, {@code POST /knowledge/answers} answers a question from it. Unit 10.2: {@code POST /knowledge/evaluations} measures retrieval
 * per chunking strategy. Versioned like the other controllers (path segment 1).
 */
@RestController
@RequestMapping(path = "/api/{version}/knowledge", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "knowledge")
class KnowledgeController {

	private final KnowledgeIngestionService ingestionService;
	private final KnowledgeService knowledgeService;
	private final KnowledgeEvaluationService evaluationService;

	KnowledgeController(final KnowledgeIngestionService ingestionService, final KnowledgeService knowledgeService,
						final KnowledgeEvaluationService evaluationService) {
		this.ingestionService = ingestionService;
		this.knowledgeService = knowledgeService;
		this.evaluationService = evaluationService;
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

	@PostMapping(path = "/evaluations", version = "v1")
	@Operation(summary = "Measure retrieval quality per chunking strategy",
			   description = "Runs the bundled question set through retrieval only (no chat model) under every chunking strategy and "
							 + "reports hit@1, hit@k, MRR, similarities and context size. Ingest first.")
	@ApiResponse(responseCode = "200", description = "Per strategy metrics and per question ranks")
	@ApiResponse(responseCode = "400", description = "top-k outside 1-20, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The embedding model or the vector store is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	KnowledgeEvaluationResponse evaluate(@RequestParam(name = "top-k", defaultValue = "4") @Min(1) @Max(20) final int topK) {
		return evaluationService.evaluate(topK);
	}
}