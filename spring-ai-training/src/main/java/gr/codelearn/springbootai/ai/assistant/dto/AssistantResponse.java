package gr.codelearn.springbootai.ai.assistant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Unit 11.1: the assistant's answer and what it did to get there.
 *
 * @param conversationId the conversation this message belongs to: the one sent, or the new one; send it again to continue
 * @param content        the answer
 * @param toolCalls      every tool call the model made, in order; empty when it answered without tools
 * @param model          the model that wrote it, as reported by the provider ({@code null} if it did not say)
 * @param usage          token counts summed over every round of the tool loop (each round resends the whole conversation)
 */
public record AssistantResponse(
		@Schema(description = "The conversation; send it with the next message") UUID conversationId,
		@Schema(description = "The answer") String content,
		@Schema(description = "The tool calls the model made, in order") List<ToolCall> toolCalls,
		@Schema(description = "The model that produced it", example = "qwen3.5:4b") @Nullable String model,
		@Schema(description = "Token usage of all rounds") TokenUsage usage) {

	/**
	 * One tool call, as the model asked for it and as it went.
	 *
	 * @param name           the tool
	 * @param arguments      the JSON arguments the model wrote
	 * @param resultChars    the length of the result sent back to the model (JSON, or the error message)
	 * @param durationMillis how long the tool took
	 * @param error          the error message the model got instead of a result, {@code null} when the call succeeded
	 */
	public record ToolCall(
			@Schema(example = "searchProducts") String name,
			@Schema(example = "{\"keywords\":\"headphones\"}") String arguments,
			@Schema(example = "812") int resultChars,
			@Schema(example = "14") long durationMillis,
			@Schema(example = "No product with SKU AUD-9999") @Nullable String error) {
	}

	/**
	 * Token counts as reported by the provider; any of them is {@code null} when the provider does not report it.
	 *
	 * @param promptTokens     tokens sent, over all rounds: the tool definitions, the question and every tool result
	 * @param completionTokens tokens generated, tool calls included
	 * @param totalTokens      prompt + completion
	 */
	public record TokenUsage(@Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Integer totalTokens) {
	}
}