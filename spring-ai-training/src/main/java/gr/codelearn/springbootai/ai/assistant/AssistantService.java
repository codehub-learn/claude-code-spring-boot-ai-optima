package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.product.CatalogTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unit 11.1: the shop assistant. Each call offers the model the catalog tools and lets Spring AI run the tool loop:
 *
 * <ol>
 *   <li>the prompt goes out with the tools' definitions (name, description, parameter schema);</li>
 *   <li>if the model answers with tool calls, Spring AI's tool calling advisor invokes them, appends the results as a tool response
 *   message, and calls the model again with the whole conversation;</li>
 *   <li>until the model answers with text. Token usage is summed over the rounds.</li>
 * </ol>
 *
 * <p>The tools are added to this call only ({@code .tools(...)}), not as defaults of the central {@code ChatClient}: the other endpoints
 * stay tool-free and don't pay for the definitions in every prompt. Each call wraps them in {@link RecordingToolCallback}s writing to a
 * list of its own, returned as {@code toolCalls}.
 */
@Service
class AssistantService {

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;
	private final ToolCallback[] catalogTools;
	private final PromptTemplate questionTemplate;

	AssistantService(final ChatClient chatClient, final CatalogTools catalogTools,
					 @Value("classpath:prompts/assistant/question.st") final Resource questionTemplate) {
		this.chatClient = chatClient;
		this.catalogTools = ToolCallbacks.from(catalogTools);
		this.questionTemplate = new PromptTemplate(questionTemplate);
	}

	AssistantResponse answer(final AssistantRequest request) {
		final List<ToolCall> calls = Collections.synchronizedList(new ArrayList<>());
		final Object[] tools = Arrays.stream(catalogTools).map(tool -> new RecordingToolCallback(tool, calls)).toArray();
		final ChatResponse response;
		try {
			response = chatClient.prompt()
								 .system(SystemPromptParams::fill)
								 .user(questionTemplate.render(Map.of("question", request.question())))
								 .tools(tools)
								 .call()
								 .chatResponse();
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new AssistantResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""), List.copyOf(calls),
									 metadata.getModel(),
									 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}
}