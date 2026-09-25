package gr.codelearn.springbootai.ai.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The knowledge base's answer and what it was based on.
 *
 * @param content the answer, citing sources by their tag ({@code [catalog:LAP-0002]}, {@code [doc:warranty]})
 * @param sources the documents retrieval put in the context, nearest first; the model may have used only some of them
 * @param model   the model that wrote it, as reported by the provider ({@code null} if it did not say)
 * @param usage   token counts; the prompt count includes every retrieved document
 */
public record KnowledgeAnswerResponse(
		@Schema(description = "The answer") String content,
		@Schema(description = "The retrieved documents, nearest first") List<Source> sources,
		@Schema(description = "The model that produced it", example = "claude-sonnet-5") @Nullable String model,
		@Schema(description = "Token usage of this call") TokenUsage usage) {

	/**
	 * One retrieved document.
	 *
	 * @param source     {@code catalog} (a product row) or {@code doc} (a bundled document)
	 * @param ref        the SKU or the document name; with the source, the tag the answer cites
	 * @param title      the product name or the document's heading
	 * @param section    unit 10.2: the Markdown section the chunk came from ({@code null} for other chunks and catalog rows)
	 * @param similarity 1 - cosine distance to the question, 1 = same direction ({@code null} if the store did not say)
	 */
	public record Source(
			@Schema(example = "catalog") String source,
			@Schema(example = "LAP-0002") String ref,
			@Schema(example = "Latitude 16 Workstation") String title,
			@Schema(example = "Payment") @Nullable String section,
			@Schema(example = "0.61") @Nullable Double similarity) {
	}

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens sent: system prompt, instructions, retrieved documents and question
	 * @param completionTokens tokens in the answer
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}