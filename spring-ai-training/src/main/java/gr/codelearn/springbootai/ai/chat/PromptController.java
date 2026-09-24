package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest;

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
 * Unit 6.3: {@code POST /api/v1/prompts}, a completion over a client-supplied list of {@code user} / {@code assistant} messages (6.4
 * removed {@code system}: the application's own system prompt is always sent first, see {@code config.ai.ChatClientConfig}). Where
 * {@code /chat-completions} hides the message model behind one {@code message} string, this endpoint exposes it: each entry becomes one
 * Spring AI {@code Message} of the matching type, and together they form the {@code Prompt}.
 *
 * <p>Versioned the same way as {@link ChatController} (path segment 1, {@code version = "v1"}).
 */
@RestController
@RequestMapping(path = "/api/{version}/prompts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "chat")
class PromptController {

	private final ChatService chatService;

	PromptController(final ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Complete an explicit list of messages",
			   description = "Stateless: the client sends the whole conversation (user and assistant turns); the system prompt is the " +
							 "application's on every call.")
	@ApiResponse(responseCode = "200", description = "The model's reply to the last user message, the model name, and the token usage")
	@ApiResponse(responseCode = "400",
				 description =
						 "No messages, too many, a blank one, an unknown role (including system and tool), the last one not from the " +
						 "user, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ChatCompletionResponse complete(@Valid @RequestBody final PromptRequest request) {
		return chatService.completePrompt(request);
	}
}