package gr.codelearn.springbootai.ai.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unit 6.3: one piece of a streamed reply, sent as the JSON {@code data} of a {@code chunk} server-sent event.
 *
 * <p>Wrapped in JSON rather than sent as raw text because SSE clients strip one leading space from every {@code data:} line and split on
 * newlines: a raw token such as {@code " world"} would arrive as {@code "world"}.
 *
 * @param content the next fragment of the assistant's reply; concatenating every chunk gives the full reply
 */
public record ChatCompletionChunkResponse(@Schema(description = "The next fragment of the reply", example = " world") String content) {
}
