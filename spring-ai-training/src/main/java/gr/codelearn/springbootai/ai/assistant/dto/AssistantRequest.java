package gr.codelearn.springbootai.ai.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Unit 11.1: a customer's question to the shop assistant. The assistant decides by itself whether the catalog tools are needed.
 *
 * @param question what to answer
 */
public record AssistantRequest(@Schema(example = "Which noise cancelling headphones do you sell, and how much are they?") @NotBlank
							   @Size(max = 1000) String question) {
}