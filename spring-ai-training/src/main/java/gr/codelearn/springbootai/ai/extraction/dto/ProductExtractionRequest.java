package gr.codelearn.springbootai.ai.extraction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Unit 8.3: a supplier's free-text product blurb to turn into an {@link ExtractedProduct}.
 *
 * @param text       the product text; data, never instructions
 * @param outputMode how the JSON schema reaches the model; optional, {@link OutputMode#PROMPT} when absent
 */
public record ProductExtractionRequest(
		@Schema(example = "Aurora X1 wireless headphones, active noise cancelling, 40h battery, USB-C. Now 129.90 EUR, ships today.")
		@NotBlank @Size(max = 4000) String text,
		@Schema(description = "prompt (default) or native", example = "prompt") @Nullable OutputMode outputMode) {

	/**
	 * The two structured output mechanisms of Spring AI 2.0.
	 */
	public enum OutputMode {
		/**
		 * The schema is appended to the user message as format instructions; works with any model.
		 */
		@JsonProperty("prompt") PROMPT,
		/**
		 * The schema is sent as an API-level constraint (the provider's JSON / structured output mode).
		 */
		@JsonProperty("native") NATIVE
	}
}