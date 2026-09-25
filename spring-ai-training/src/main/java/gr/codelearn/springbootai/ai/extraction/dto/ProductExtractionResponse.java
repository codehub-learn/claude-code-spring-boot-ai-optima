package gr.codelearn.springbootai.ai.extraction.dto;

import gr.codelearn.springbootai.ai.extraction.dto.ProductExtractionRequest.OutputMode;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * The mapped and validated product plus the metadata the provider reported for it.
 *
 * @param product    the extracted product
 * @param outputMode the mechanism that was used
 * @param model      the model that produced it, as reported by the provider ({@code null} if it did not say)
 * @param usage      token counts, summed over every attempt when the output had to be repaired
 */
public record ProductExtractionResponse(
		@Schema(description = "The extracted product") ExtractedProduct product,
		@Schema(description = "How the schema reached the model") OutputMode outputMode,
		@Schema(description = "The model that produced it", example = "claude-sonnet-5") @Nullable String model,
		@Schema(description = "Token usage, summed over all attempts") TokenUsage usage) {

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens in the prompts sent (the text, the instructions and, in prompt mode, the schema)
	 * @param completionTokens tokens in the generated JSON
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}