package gr.codelearn.springbootai.ai.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The model's answer plus the metadata the provider reported for it.
 *
 * @param content the assistant's reply text
 * @param model   the model that answered, as reported by the provider ({@code null} if it did not say)
 * @param usage   token counts for this single call
 */
public record ChatCompletionResponse(
		@Schema(description = "The assistant's reply") String content,
		@Schema(description = "The model that produced the reply", example = "claude-sonnet-5") @Nullable String model,
		@Schema(description = "Token usage of this call") TokenUsage usage) {

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens in the prompt sent to the model
	 * @param completionTokens tokens in the generated reply
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}
