package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.assistant.RecordingToolCallback;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.config.ai.ToolErrorProcessor;
import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerRequest;
import gr.codelearn.springbootai.ai.mcp.dto.McpAnswerResponse;
import gr.codelearn.springbootai.ai.mcp.dto.McpToolResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Unit 11.4: uses the tools of the connected MCP servers.
 *
 * <p>The MCP client starter turns every tool of every configured server into a {@link ToolCallback} and publishes them as one
 * {@link ToolCallbackProvider} bean. From there it is the tool loop of 11.1 unchanged: the definitions go out with the prompt, the model
 * asks for a call, the callback sends a {@code tools/call} request to the server over HTTP and the result goes back to the model.
 *
 * <p>Nothing about the tools is known at startup ({@code spring.ai.mcp.client.initialized: false}, so the application starts with the
 * server down). The provider connects and lists the tools the first time a request needs them; while the server is down that fails the
 * request with {@link McpServerUnavailableException} (502) and nothing else, and the first request after it is back succeeds. Once listed,
 * the provider keeps the list (until the server sends a tools-changed notification), so a server that dies later is noticed by the failing
 * tool call, which the model is told about like any tool failure, not by the listing.
 *
 * <p>Known limit: when a remote tool reports an error, the MCP client throws with the server's message inside a generic text, which
 * {@link ToolErrorProcessor} does not recognise, so the model is told the tool failed unexpectedly (the server's own message is in the
 * log). Safe, but less helpful than 11.3's messages for local tools.
 */
@Service
class McpService {

	private static final Logger log = LoggerFactory.getLogger(McpService.class);

	private final ChatClient chatClient;
	private final ObjectProvider<ToolCallbackProvider> mcpTools;
	private final ToolErrorProcessor toolErrors;

	McpService(final ChatClient chatClient, final ObjectProvider<ToolCallbackProvider> mcpTools, final ToolErrorProcessor toolErrors) {
		this.chatClient = chatClient;
		this.mcpTools = mcpTools;
		this.toolErrors = toolErrors;
	}

	List<McpToolResponse> listTools() {
		return discover().stream()
						 .map(ToolCallback::getToolDefinition)
						 .map(tool -> new McpToolResponse(tool.name(), tool.description(), tool.inputSchema()))
						 .toList();
	}

	McpAnswerResponse answer(final McpAnswerRequest request) {
		final List<ToolCall> calls = Collections.synchronizedList(new ArrayList<>());
		final ToolCallback[] recording = discover().stream()
												   .map(tool -> new RecordingToolCallback(tool, calls, toolErrors::describe))
												   .toArray(ToolCallback[]::new);
		final ChatResponse response;
		try {
			response = chatClient.prompt()
								 .system(SystemPromptParams::fill)
								 .user(request.question())
								 .toolCallbacks(recording)
								 .call()
								 .chatResponse();
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException("The chat model is unavailable, try again later", ex);
		}
		if (response == null) {
			throw new ChatModelUnavailableException("The chat model gave no answer", null);
		}
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new McpAnswerResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""), List.copyOf(calls),
									 metadata.getModel(),
									 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	private List<ToolCallback> discover() {
		final ToolCallbackProvider provider = mcpTools.getIfUnique();
		if (provider == null) {
			throw new McpServerUnavailableException("No MCP client is configured (spring.ai.mcp.client.enabled is false)", null);
		}
		try {
			return List.of(provider.getToolCallbacks());
		} catch (RuntimeException ex) {
			log.warn("Cannot list the MCP server's tools: {}", ex.toString());
			throw new McpServerUnavailableException("The MCP server is unavailable, try again later", ex);
		}
	}
}