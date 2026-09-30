package gr.codelearn.springbootai.ai.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Unit 11.1: a customer's message to the shop assistant. The assistant decides by itself whether the tools are needed. Since the memory
 * follow-up it is one message of a conversation: the assistant remembers what was said before in the same conversation.
 *
 * @param question       what the customer says
 * @param conversationId the conversation to continue, from an earlier answer or {@code POST /api/v1/conversations}; omitted, a new
 *                       conversation starts and its id comes back in the answer
 */
public record AssistantRequest(
		@Schema(example = "Which noise cancelling headphones do you sell, and how much are they?") @NotBlank @Size(
				max = 1000) String question,
		@Schema(description = "The conversation to continue; omit to start a new one") @Nullable UUID conversationId) {
}