package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;
import gr.codelearn.springbootai.ai.conversation.ConversationNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Unit 11.1: {@code POST /assistant/answers}, a question answered by a model that may call the tools. Versioned like the other controllers
 * (path segment 1).
 *
 * <p><b>Which conversation a message belongs to</b>, in this order:
 * <ol>
 *   <li>the {@code conversationId} in the body, when there is one;</li>
 *   <li>otherwise the {@value #COOKIE} cookie the last answer set, so a browser-like client (Swagger UI, IntelliJ's HTTP client) continues
 *   the conversation without the caller copying ids around. A cookie whose conversation no longer exists is ignored, not an error;</li>
 *   <li>otherwise a new conversation.</li>
 * </ol>
 * Every answer sets the cookie to the conversation it belongs to. The cookie is {@code HttpOnly} (scripts cannot read it), {@code
 * SameSite=Lax}, limited to this controller's path, a session cookie, and {@code Secure} when the request came over HTTPS. Clients
 * that keep no cookies (curl) pass the id from the answer themselves. There is no authentication in this reference application: whoever
 * has a conversation's id can continue it, with or without the cookie.
 */
@RestController
@RequestMapping(path = "/api/{version}/assistant", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "assistant")
class AssistantController {

	static final String COOKIE = "assistant-conversation";

	/**
	 * The cookie is only sent to this controller's endpoints.
	 */
	private static final String PATH = "/api/v1/assistant";

	private final AssistantService assistantService;

	AssistantController(final AssistantService assistantService) {
		this.assistantService = assistantService;
	}

	@PostMapping(path = "/answers", version = "v1", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Talk to the shop assistant",
			   description = "The model may call the tools (search products, get a product by SKU, estimate delivery) before answering. " +
							 "The response lists every tool call it made. The assistant remembers the conversation: it continues the " +
							 "conversationId in the body, else the one in the assistant-conversation cookie (set by every answer, so " +
							 "Swagger UI continues by itself), else it starts a new one.")
	@ApiResponse(responseCode = "200", description = "The answer, the tool calls, the model name, and the token usage of all rounds")
	@ApiResponse(responseCode = "400", description = "A blank or oversized question, or an unsupported API version",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "The conversationId in the body does not exist",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "502", description = "The chat model is unreachable or failed",
				 content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
									schema = @Schema(implementation = ProblemDetail.class)))
	ResponseEntity<AssistantResponse> answer(@Valid @RequestBody final AssistantRequest request,
											 @CookieValue(name = COOKIE, required = false) final @Nullable String cookie,
											 final HttpServletRequest http) {
		final AssistantResponse response = request.conversationId() != null ? assistantService.answer(request)
																			: continueOrStart(request, parse(cookie));
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(response.conversationId().toString(), http, -1).toString()).body(
				response);
	}

	@DeleteMapping(path = "/conversation", version = "v1")
	@Operation(summary = "Forget the current conversation",
			   description = "Deletes the conversation of the assistant-conversation cookie (its memory too) and clears the cookie, so " +
							 "the "
							 + "next message starts a new conversation. Nothing to do without the cookie.")
	@ApiResponse(responseCode = "204", description = "The conversation is gone and the cookie cleared")
	ResponseEntity<Void> forget(@CookieValue(name = COOKIE, required = false) final @Nullable String cookie,
								final HttpServletRequest http) {
		final UUID id = parse(cookie);
		if (id != null) {
			assistantService.forget(id);
		}
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", http, 0).toString()).build();
	}

	/**
	 * The cookie's conversation, or a new one when there is no cookie or its conversation is gone (deleted, or the database was reset).
	 */
	private AssistantResponse continueOrStart(final AssistantRequest request, final @Nullable UUID fromCookie) {
		if (fromCookie == null) {
			return assistantService.answer(request);
		}
		try {
			return assistantService.answer(new AssistantRequest(request.question(), fromCookie));
		} catch (ConversationNotFoundException ex) {
			return assistantService.answer(request);
		}
	}

	private static @Nullable UUID parse(final @Nullable String cookie) {
		if (cookie == null) {
			return null;
		}
		try {
			return UUID.fromString(cookie);
		} catch (IllegalArgumentException ex) {
			// A cookie is client input like any other: garbage means no conversation, not an error.
			return null;
		}
	}

	private static ResponseCookie cookie(final String value, final HttpServletRequest http, final long maxAgeSeconds) {
		return ResponseCookie.from(COOKIE, value)
							 .httpOnly(true)
							 .secure(http.isSecure())
							 .sameSite("Lax")
							 .path(PATH)
							 .maxAge(maxAgeSeconds)
							 .build();
	}
}