package gr.codelearn.springbootai.ai.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A question for the knowledge base. Only the question: the context comes from retrieval, not from the caller (compare
 * {@code answer.dto.AnswerRequest}, 8.2).
 *
 * @param question what to answer
 */
public record KnowledgeAnswerRequest(
		@Schema(example = "Which laptop should I buy for 3D rendering, and how long is its warranty?") @NotBlank @Size(max = 1000)
		String question) {
}