package gr.codelearn.springbootai.ai.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The model's reply to one turn of a stateful conversation.
 *
 * @param conversationId the conversation it belongs to
 * @param turn           which turn this was, from 1
 * @param content        the reply
 * @param model          the model that wrote it, as reported by the provider ({@code null} if it did not say)
 * @param usage          token counts for this turn; the prompt count grows every turn, because the whole history is sent again
 */
public record ConversationReplyResponse(
		@Schema(description = "The conversation's id") UUID conversationId,
		@Schema(description = "The turn number, from 1", example = "2") int turn,
		@Schema(description = "The model's reply") String content,
		@Schema(description = "The model that produced it", example = "claude-sonnet-5") @Nullable String model,
		@Schema(description = "Token usage of this turn") TokenUsage usage) {

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens sent: the system prompt, every earlier turn, and the new message
	 * @param completionTokens tokens in the reply
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}