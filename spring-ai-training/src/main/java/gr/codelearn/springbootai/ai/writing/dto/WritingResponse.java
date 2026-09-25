package gr.codelearn.springbootai.ai.writing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The text the model wrote plus the metadata the provider reported for it.
 *
 * @param content the generated text
 * @param model   the model that wrote it, as reported by the provider ({@code null} if it did not say)
 * @param usage   token counts for this single call
 */
public record WritingResponse(
		@Schema(description = "The generated text") String content,
		@Schema(description = "The model that produced it", example = "claude-sonnet-5") @Nullable String model,
		@Schema(description = "Token usage of this call") TokenUsage usage) {

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens in the prompt sent to the model (the rendered template)
	 * @param completionTokens tokens in the generated text
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}