package gr.codelearn.springbootai.ai.writing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Unit 8.1: the values for the {@code prompts/writing/product-description.st} template.
 *
 * @param name     the product's name
 * @param category the catalog category it belongs to
 * @param features the facts the description must mention, rendered as a bullet list; the model is told not to invent others
 * @param tone     the writing style
 * @param language the language to write in; optional, English when absent
 */
public record ProductDescriptionRequest(
		@Schema(example = "Aurora X1 noise-cancelling headphones") @NotBlank @Size(max = 200) String name,
		@Schema(example = "Audio") @NotBlank @Size(max = 100) String category,
		@Schema(example = "[\"40-hour battery\", \"active noise cancelling\", \"USB-C fast charging\"]")
		@NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 200) String> features,
		@Schema(example = "friendly") @NotNull Tone tone,
		@Schema(description = "Language to write in (optional, English when absent)", example = "Greek") @Size(max = 50)
		@Nullable String language) {

	/**
	 * The writing styles the template offers. The lower-case JSON value is also the word the template puts in the prompt.
	 */
	public enum Tone {
		@JsonProperty("friendly") FRIENDLY,
		@JsonProperty("professional") PROFESSIONAL,
		@JsonProperty("playful") PLAYFUL
	}
}