package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionChunkResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionRequest;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Unit 6.2: the first chat endpoint, {@code POST /api/v1/chat-completions}, answered in one JSON body. Unit 6.3 adds a streamed
 * representation of the same resource ({@code Accept: text/event-stream}).
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

	private static final Logger log = LoggerFactory.getLogger(ChatController.class);

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

	/**
	 * Unit 6.3: the same resource, streamed. Content negotiation picks this handler for {@code Accept: text/event-stream}; the
	 * method-level
	 * {@code produces} overrides the class-level JSON one. Spring MVC subscribes to the {@link Flux} and writes each element as a
	 * server-sent event as soon as it arrives.
	 *
	 * <p>A model failure after the stream has started cannot become a 502 any more: the 200 status and the {@code text/event-stream}
	 * headers are already on the wire, so {@code ApiExceptionHandler} can't answer with a {@link ProblemDetail} response. The failure is
	 * sent in-band instead, as a final {@code error} event whose data is the same {@link ProblemDetail} the blocking endpoint would
	 * return.
	 */
	@PostMapping(version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Stream the reply to one message",
			   description = "Same input as the blocking call. Emits 'chunk' events ({\"content\": ...}) as the model generates them, or" +
							 " a final 'error' event carrying a ProblemDetail if the model fails mid-stream.")
	@ApiResponse(responseCode = "200", description = "A text/event-stream of 'chunk' events, possibly ending with one 'error' event",
				 content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
									schema = @Schema(implementation = ChatCompletionChunkResponse.class)))
	@ApiResponse(responseCode = "400", description = "Blank or oversized message, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	Flux<ServerSentEvent<Object>> stream(@Valid @RequestBody final ChatCompletionRequest request) {
		return chatService.stream(request).map(chunk -> ServerSentEvent.<Object>builder(chunk).event("chunk").build()).onErrorResume(
				ChatModelUnavailableException.class, ex -> {
					log.warn("Chat model stream failed: {}", ex.getMessage(), ex.getCause());
					final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
					return Flux.just(ServerSentEvent.<Object>builder(problem).event("error").build());
				});
	}
}