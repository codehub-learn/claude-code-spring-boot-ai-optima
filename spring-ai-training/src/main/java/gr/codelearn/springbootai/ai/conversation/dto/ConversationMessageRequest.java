package gr.codelearn.springbootai.ai.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Unit 9.1: the next user message of a stateful conversation. Only the new message: compare {@code chat.dto.PromptRequest} (6.3), where the
 * client sends the whole history every time.
 *
 * @param content what the user says
 */
public record ConversationMessageRequest(
		@Schema(example = "And what does it add over Spring Boot 3?") @NotBlank @Size(max = 4000) String content) {
}