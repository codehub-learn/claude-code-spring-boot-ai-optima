package gr.codelearn.springbootai.ai.mcp.dto;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The answer and the MCP tool calls behind it. {@code ToolCall} and {@code TokenUsage} are the assistant's records: a tool call is recorded
 * the same way whether the tool is local or remote.
 *
 * @param content   the answer
 * @param toolCalls every tool call the model made, in order; empty when it answered without tools
 * @param model     the model that wrote it, as reported by the provider
 * @param usage     token counts summed over every round of the tool loop
 */
public record McpAnswerResponse(
		@Schema(description = "The answer") String content,
		@Schema(description = "The MCP tool calls the model made, in order") List<ToolCall> toolCalls,
		@Schema(description = "The model that produced it", example = "qwen3.5:4b") @Nullable String model,
		@Schema(description = "Token usage of all rounds") TokenUsage usage) {
}