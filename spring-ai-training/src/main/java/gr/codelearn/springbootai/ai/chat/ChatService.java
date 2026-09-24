package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionChunkResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionRequest;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse.TokenUsage;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest;
import gr.codelearn.springbootai.ai.chat.dto.PromptRequest.PromptMessage;
import gr.codelearn.springbootai.ai.chat.internal.ChatModelUnavailableException;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Units 6.2 / 6.3: chat completions through the central {@link ChatClient} (built in {@code config.ai.ChatClientConfig}).
 *
 * <ul>
 *   <li>{@link #complete(ChatCompletionRequest)} (6.2): the fluent shortcut, {@code .user(text)} builds the {@link UserMessage} for us
 *   .</li>
 *   <li>{@link #completePrompt(PromptRequest)} (6.3): the same call, but the {@link Prompt} is assembled by hand from typed
 *   {@link Message}s,
 *   which is exactly what the fluent API does under the hood. The {@link Prompt} still goes through {@code chatClient.prompt(prompt)}, so
 *   the default advisors keep applying.</li>
 *   <li>{@link #stream(ChatCompletionRequest)} (6.3): {@code stream()} instead of {@code call()}, a {@link Flux} of fragments emitted as
 *   the model generates them, instead of one {@link ChatResponse} after the model has finished.</li>
 * </ul>
 *
 * <p>{@code call().chatResponse()} rather than {@code call().content()}: the full {@link ChatResponse} also carries the provider's
 * metadata (model name, token usage), which the response exposes. No {@code @Transactional}: there is no persistence here, and CLAUDE.md
 * keeps AI/network calls outside transactions anyway.
 */
@Service
class ChatService {

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;

	ChatService(final ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	ChatCompletionResponse complete(final ChatCompletionRequest request) {
		return toResponse(callModel(() -> chatClient.prompt().user(request.message()).call().chatResponse()));
	}

	ChatCompletionResponse completePrompt(final PromptRequest request) {
		final List<Message> messages = request.messages().stream().map(ChatService::toMessage).toList();
		// No ChatOptions: the model, max tokens, etc. come from spring.ai.anthropic.chat.* (a Prompt can override them per call).
		final Prompt prompt = new Prompt(messages);
		return toResponse(callModel(() -> chatClient.prompt(prompt).call().chatResponse()));
	}

	Flux<ChatCompletionChunkResponse> stream(final ChatCompletionRequest request) {
		// defer: nothing, not even building the request, happens until the web layer subscribes.
		return Flux.defer(() -> chatClient.prompt().user(request.message()).stream().content())
				   .map(ChatCompletionChunkResponse::new)
				   .onErrorMap(ex -> !(ex instanceof ChatModelUnavailableException),
							   ex -> new ChatModelUnavailableException(UNAVAILABLE, ex));
	}

	/**
	 * One API role to one Spring AI message type. {@code ToolResponseMessage} (role {@code tool}) has no case: the application creates it
	 * when it runs a tool the model asked for (U11), a client never sends one.
	 */
	private static Message toMessage(final PromptMessage message) {
		return switch (message.role()) {
			case SYSTEM -> new SystemMessage(message.content());
			case USER -> new UserMessage(message.content());
			case ASSISTANT -> new AssistantMessage(message.content());
		};
	}

	private static ChatResponse callModel(final Supplier<@Nullable ChatResponse> call) {
		final ChatResponse response;
		try {
			response = call.get();
		} catch (RuntimeException ex) {
			// Connection refused, provider HTTP error, retries exhausted: all surface as unchecked exceptions from the model call.
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		return response;
	}

	private static ChatCompletionResponse toResponse(final ChatResponse response) {
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new ChatCompletionResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
										  metadata.getModel(),
										  new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}
}