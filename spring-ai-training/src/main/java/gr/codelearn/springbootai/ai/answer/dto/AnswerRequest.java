package gr.codelearn.springbootai.ai.answer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Unit 8.2: a question plus the context to answer it from. The question and the documents are per-call data; the instructions around them
 * are fixed in {@code prompts/answer/question.st} and never come from the client.
 *
 * <p>The limits keep the whole prompt well inside the local model's 8192-token context (7.2): Ollama silently drops the start of a
 * prompt that does not fit, and the start is where the instructions are.
 *
 * @param question  what to answer
 * @param documents the context to answer from; optional: without it the prompt is instruction-only and the model answers from its own
 *                  knowledge
 */
public record AnswerRequest(
		@Schema(example = "How long is the warranty on the Aurora X1?") @NotBlank @Size(max = 1000) String question,
		@Schema(description = "Reference documents to answer from (optional)") @Size(max = 5)
		@Nullable List<@Valid Document> documents) {

	/**
	 * One piece of context. The model cites it by its position in the list, starting at 1.
	 *
	 * @param title a short label for the document
	 * @param text  the document's content, treated as data, not instructions
	 */
	public record Document(
			@Schema(example = "Aurora X1 warranty terms") @NotBlank @Size(max = 200) String title,
			@Schema(example = "The Aurora X1 comes with a 2-year limited warranty.") @NotBlank @Size(max = 4000) String text) {
	}
}