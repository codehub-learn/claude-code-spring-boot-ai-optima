package gr.codelearn.springbootai.ai.answer;

import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest;
import gr.codelearn.springbootai.ai.answer.dto.AnswerResponse;

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
 * Unit 8.2: {@code POST /api/v1/answers}. The client sends a question and, optionally, the documents to answer it from; never instructions.
 * Stateless, and nothing is stored, hence {@code 200} rather than {@code 201}.
 *
 * <p>Versioned the same way as {@code chat.ChatController} (path segment 1, {@code version = "v1"}).
 */
@RestController
@RequestMapping(path = "/api/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "answer")
class AnswerController {

	private final AnswerService answerService;

	AnswerController(final AnswerService answerService) {
		this.answerService = answerService;
	}

	@PostMapping(path = "/answers", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Answer a question, from the given documents if any",
			   description = "With documents, the model answers only from them and cites them by number. Without, it answers from its own "
							 + "knowledge (an instruction-only prompt).")
	@ApiResponse(responseCode = "200", description = "The answer, the model name, and the token usage")
	@ApiResponse(responseCode = "400",
				 description = "A blank or oversized question or document, more than 5 documents, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	AnswerResponse answer(@Valid @RequestBody final AnswerRequest request) {
		return answerService.answer(request);
	}
}