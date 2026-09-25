package gr.codelearn.springbootai.ai.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * A newly created conversation. The id is the only thing a client needs to keep; the history stays on the server.
 *
 * @param id        where the conversation lives: {@code /api/v1/conversations/{id}}
 * @param createdAt when it was created
 */
public record ConversationResponse(
		@Schema(description = "The conversation's id") UUID id,
		@Schema(description = "When it was created") Instant createdAt) {
}