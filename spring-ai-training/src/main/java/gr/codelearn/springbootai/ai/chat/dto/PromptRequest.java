package gr.codelearn.springbootai.ai.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Unit 6.3: an explicit, ordered list of role-tagged messages ({@code user} / {@code assistant} since 6.4), which {@code ChatService} turns
 * one-to-one into Spring AI {@code Message}s and wraps in a {@code Prompt}. The client owns the whole conversation: nothing is remembered
 * between calls (memory is U9).
 *
 * @param messages the conversation so far, oldest first; the last one must come from the user, since that is the turn the model answers
 */
public record PromptRequest(
		@Schema(description = "The conversation so far, oldest first. The last message must have role 'user'.")
		@NotEmpty
		@Size(max = 20)
		List<@Valid @NotNull PromptMessage> messages) {

	/**
	 * Cross-field rule, checked by Bean Validation like any getter. A missing list or a null last element passes here on purpose: the
	 * {@code NotEmpty} / {@code NotNull} constraints already report those, so each problem is reported once. {@code JsonIgnore} keeps it
	 * out of the JSON and the OpenAPI schema. The null checks stay although the package is null-marked: Jackson can still bind a null from
	 * the wire.
	 */
	@JsonIgnore
	@AssertTrue(message = "the last message must have role 'user'")
	@SuppressWarnings("ConstantValue")
	boolean isLastMessageFromUser() {
		if (messages == null || messages.isEmpty() || messages.getLast() == null) {
			return true;
		}
		return messages.getLast().role() == Role.USER;
	}

	/**
	 * One turn of the conversation.
	 *
	 * @param role    who said it
	 * @param content what was said
	 */
	public record PromptMessage(
			@Schema(description = "Who said it", example = "user") @NotNull Role role,
			@Schema(description = "What was said", example = "And in one word?") @NotBlank @Size(max = 4000) String content) {
	}

	/**
	 * The roles a client may send. {@code system} is absent since 6.4: system instructions come only from the application's template (a
	 * client one would sit next to them and compete). {@code tool} is absent too: tool results are produced by the application (U11).
	 * Either one is rejected as an unreadable body (400).
	 */
	public enum Role {
		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}
}