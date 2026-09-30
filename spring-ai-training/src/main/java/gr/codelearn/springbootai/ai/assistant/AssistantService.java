package gr.codelearn.springbootai.ai.assistant;

import gr.codelearn.springbootai.ai.assistant.dto.AssistantRequest;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.TokenUsage;
import gr.codelearn.springbootai.ai.assistant.dto.AssistantResponse.ToolCall;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.config.ai.ToolErrorProcessor;
import gr.codelearn.springbootai.ai.conversation.ConversationNotFoundException;
import gr.codelearn.springbootai.ai.conversation.ConversationService;
import gr.codelearn.springbootai.ai.delivery.DeliveryTools;
import gr.codelearn.springbootai.ai.product.CatalogTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Unit 11.1: the shop assistant. Each call offers the model the tools and lets Spring AI run the tool loop:
 *
 * <ol>
 *   <li>the prompt goes out with the tools' definitions (name, description, parameter schema);</li>
 *   <li>if the model answers with tool calls, Spring AI's tool calling advisor invokes them, appends the results as a tool response
 *   message, and calls the model again with the whole conversation;</li>
 *   <li>until the model answers with text. Token usage is summed over the rounds.</li>
 * </ol>
 *
 * <p>Since 11.3 the tools are the catalog tools and the delivery tool. They are added to this call only ({@code .tools(...)}), not as
 * defaults of the central {@code ChatClient}: the other endpoints stay tool-free and don't pay for the definitions in every prompt. Each
 * call wraps them in {@link RecordingToolCallback}s writing to a list of its own, returned as {@code toolCalls}.
 *
 * <p><b>Memory.</b> Each message is a turn of a conversation, run through {@link ConversationService#chat}: the conversation feature
 * puts the remembered messages before the new one, saves the new one and the answer, and undoes a failed turn. Without a
 * {@code conversationId} a new conversation starts (and is removed again if its first turn fails, so a failed first message leaves
 * nothing behind). Two things follow from what memory stores:
 *
 * <ul>
 *   <li>the shop instructions are in the <em>system</em> prompt, not in the user message as before: memory saves the user message, and
 *   an instruction block in every remembered message would be sent again and again, and show in the history;</li>
 *   <li>only the question and the final answer are remembered, not the tool calls and their results (the memory advisor sits outside the
 *   tool loop). A follow-up that needs fresh data has the model call the tool again, which is the point of the "fresh data" rule in
 *   the instructions.</li>
 * </ul>
 */
@Service
class AssistantService {

	private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

	private final ConversationService conversationService;
	private final ToolCallback[] tools;
	private final ToolErrorProcessor toolErrors;
	private final String systemPrompt;

	AssistantService(final ConversationService conversationService, final CatalogTools catalogTools, final DeliveryTools deliveryTools,
					 final ToolErrorProcessor toolErrors, @Value("classpath:prompts/chat-system.st") final Resource chatSystemPrompt,
					 @Value("classpath:prompts/assistant/instructions.st") final Resource instructions) {
		this.conversationService = conversationService;
		this.tools = ToolCallbacks.from(catalogTools, deliveryTools);
		this.toolErrors = toolErrors;
		// The application's rules first, then the shop's: a per-call .system(...) replaces the default text, so the default is repeated
		// here (with its {today} placeholder, filled per call) instead of being lost.
		this.systemPrompt = read(chatSystemPrompt) + "\n\n" + read(instructions);
	}

	AssistantResponse answer(final AssistantRequest request) {
		final boolean started = request.conversationId() == null;
		final UUID conversationId = started ? conversationService.start() : request.conversationId();
		final List<ToolCall> calls = Collections.synchronizedList(new ArrayList<>());
		final Object[] recordingTools = Arrays.stream(tools)
											  .map(tool -> new RecordingToolCallback(tool, calls, toolErrors::describe))
											  .toArray();
		final ChatResponse response;
		try {
			response = conversationService.chat(conversationId, prompt -> prompt.system(system -> {
				system.text(systemPrompt);
				SystemPromptParams.fill(system);
			}).user(request.question()).tools(recordingTools));
		} catch (RuntimeException ex) {
			if (started) {
				conversationService.delete(conversationId);
			}
			throw ex;
		}
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new AssistantResponse(conversationId, Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
									 List.copyOf(calls), metadata.getModel(),
									 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	/**
	 * Deletes the conversation and its memory; a conversation that is already gone is not an error (the goal, no conversation, holds).
	 */
	void forget(final UUID conversationId) {
		try {
			conversationService.delete(conversationId);
		} catch (ConversationNotFoundException ex) {
			log.debug("Conversation {} was already gone", conversationId);
		}
	}

	private static String read(final Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8).strip();
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + resource, ex);
		}
	}
}
