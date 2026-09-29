package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.List;
import java.util.Objects;

/**
 * Unit 11.1: wraps a tool so each call the model makes is recorded (name, arguments, result size, time, error) for the response. A
 * decorator around Spring AI's {@link ToolCallback} rather than code in the tool: {@code CatalogTools} stays a plain capability, and the
 * recording is per request (a fresh list per call of the assistant), so concurrent requests never see each other's calls.
 *
 * <p>An exception is recorded and rethrown unchanged: Spring AI's tool calling manager catches it and, by default, sends its message to
 * the model as the tool's result.
 */
final class RecordingToolCallback implements ToolCallback {

	private final ToolCallback delegate;
	private final List<ToolCall> calls;

	RecordingToolCallback(final ToolCallback delegate, final List<ToolCall> calls) {
		this.delegate = delegate;
		this.calls = calls;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(final String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(final String toolInput, final @Nullable ToolContext toolContext) {
		final String name = delegate.getToolDefinition().name();
		final long start = System.nanoTime();
		try {
			final String result = delegate.call(toolInput, toolContext);
			calls.add(new ToolCall(name, toolInput, result.length(), millisSince(start), null));
			return result;
		} catch (RuntimeException ex) {
			final String message = Objects.requireNonNullElse(ex.getMessage(), ex.getClass().getSimpleName());
			calls.add(new ToolCall(name, toolInput, message.length(), millisSince(start), message));
			throw ex;
		}
	}

	private static long millisSince(final long start) {
		return (System.nanoTime() - start) / 1_000_000;
	}
}