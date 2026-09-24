package gr.codelearn.springbootai.ai.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One user message sent to the chat model.
 *
 * @param message the user's text; the size cap keeps a single request from blowing the model's context window or the token budget
 */
public record ChatCompletionRequest(
		@Schema(description = "The user message", example = "Explain Spring AI's ChatClient in one sentence.")
		@NotBlank
		@Size(max = 4000)
		String message) {
}
