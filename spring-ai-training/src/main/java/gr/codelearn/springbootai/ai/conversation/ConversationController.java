package gr.codelearn.springbootai.ai.conversation;

import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageRequest;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Unit 9.1: a conversation is a resource, so its state is visible in the API: {@code POST /conversations} creates one ({@code 201} +
 * {@code Location}), {@code POST /conversations/{id}/messages} takes a turn, {@code GET} the same path reads the history,
 * {@code DELETE /conversations/{id}} ends it. Compare the stateless {@code chat} endpoints, which have no id and no URL for state.
 *
 * <p>Versioned the same way as {@code chat.ChatController} (path segment 1, {@code version = "v1"}).
 */
@RestController
@RequestMapping(path = "/api/{version}/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "conversation")
class ConversationController {

	private final ConversationService conversationService;

	ConversationController(final ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@PostMapping(version = "v1")
	@Operation(summary = "Start a conversation", description = "Creates an empty server-side conversation and returns its id.")
	@ApiResponse(responseCode = "201", description = "Created; the Location header points at the conversation")
	@ApiResponse(responseCode = "409", description = "Too many open conversations",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<ConversationResponse> create() {
		final ConversationResponse conversation = conversationService.create();
		final URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(conversation.id()).toUri();
		return ResponseEntity.created(location).body(conversation);
	}

	@PostMapping(path = "/{id}/messages", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Send the next message",
			   description = "The model sees the system prompt, every earlier turn of this conversation, and the new message.")
	@ApiResponse(responseCode = "200", description = "The reply, the turn number, the model name, and the token usage")
	@ApiResponse(responseCode = "400", description = "A blank or oversized message, a malformed id, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No such conversation",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The conversation reached its turn limit",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed; the turn is not stored",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ConversationReplyResponse send(@PathVariable final UUID id, @Valid @RequestBody final ConversationMessageRequest request) {
		return conversationService.send(id, request);
	}

	@GetMapping(path = "/{id}/messages", version = "v1")
	@Operation(summary = "Read the history", description = "The stored turns in order: user, assistant, user, ...")
	@ApiResponse(responseCode = "200", description = "The messages")
	@ApiResponse(responseCode = "404", description = "No such conversation",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	List<ConversationMessageResponse> history(@PathVariable final UUID id) {
		return conversationService.history(id);
	}

	@DeleteMapping(path = "/{id}", version = "v1")
	@Operation(summary = "End a conversation", description = "Drops the conversation and its history.")
	@ApiResponse(responseCode = "204", description = "Deleted")
	@ApiResponse(responseCode = "404", description = "No such conversation",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<Void> delete(@PathVariable final UUID id) {
		conversationService.delete(id);
		return ResponseEntity.noContent().build();
	}
}