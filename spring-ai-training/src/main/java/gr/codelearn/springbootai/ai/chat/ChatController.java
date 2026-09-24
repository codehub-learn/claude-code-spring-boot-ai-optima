package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionRequest;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;

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
 * Unit 6.2: the first chat endpoint, {@code POST /api/v1/chat-completions}.
 *
 * <p>API versioning is Spring Framework 7's built-in support rather than a literal {@code v1} in the path: {@code application.yml} tells
 * MVC to read the version from path segment 1 ({@code spring.mvc.apiversion.use.path-segment}), the mapping captures that segment as
 * {@code {version}}, and {@code version = "v1"} binds this handler to version 1. A {@code v2} handler can later sit on the same path next
 * to it; an unsupported version is rejected with a 400 {@link ProblemDetail} before any handler runs.
 *
 * <p>The default semantic version parser strips an optional {@code v} prefix, so {@code "v1"}, {@code "1"} and the {@code supported: 1}
 * property all mean the same version. {@code "v1"} is written here because springdoc substitutes the literal into the documented path:
 * {@code "1"} would publish {@code /api/1/chat-completions} (valid, but not the {@code /api/v1} convention).
 */
@RestController
@RequestMapping(path = "/api/{version}/chat-completions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "chat")
class ChatController {

	private final ChatService chatService;

	ChatController(final ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Send one message to the chat model", description = "Single turn, no memory: each call is independent.")
	@ApiResponse(responseCode = "200", description = "The model's reply, the model name, and the token usage")
	@ApiResponse(responseCode = "400", description = "Blank or oversized message, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
	                                schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
	                                schema = @Schema(implementation = ProblemDetail.class)))
	ChatCompletionResponse complete(@Valid @RequestBody final ChatCompletionRequest request) {
		return chatService.complete(request);
	}
}