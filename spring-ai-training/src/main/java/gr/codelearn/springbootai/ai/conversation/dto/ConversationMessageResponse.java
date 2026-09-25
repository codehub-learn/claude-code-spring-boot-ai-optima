package gr.codelearn.springbootai.ai.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One message of a conversation's history, as the server keeps it.
 *
 * @param role    who said it
 * @param content what was said
 */
public record ConversationMessageResponse(
		@Schema(description = "user or assistant") Role role,
		@Schema(description = "The message text") String content) {

	/**
	 * The two roles a stored turn has; the system prompt is the application's and is not part of the history.
	 */
	public enum Role {
		@JsonProperty("user") USER,
		@JsonProperty("assistant") ASSISTANT
	}
}