package gr.codelearn.springbootai.ai.writing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Unit 8.1: the values for the {@code prompts/writing/summary.st} template.
 *
 * @param text     the text to summarize
 * @param maxWords upper bound on the summary's length
 * @param audience who the summary is for; optional, the template leaves the sentence out when it is absent
 */
public record SummaryRequest(
		@Schema(description = "The text to summarize") @NotBlank @Size(max = 20_000) String text,
		@Schema(description = "At most this many words", example = "50") @NotNull @Min(10) @Max(300) Integer maxWords,
		@Schema(description = "Who the summary is for (optional)", example = "junior Java developers") @Size(max = 100)
		@Nullable String audience) {
}