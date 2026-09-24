package gr.codelearn.springbootai.ai.chat;

import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionRequest;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse;
import gr.codelearn.springbootai.ai.chat.dto.ChatCompletionResponse.TokenUsage;
import gr.codelearn.springbootai.ai.chat.internal.ChatModelUnavailableException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Unit 6.2: one user message in, one completion out, through the central {@link ChatClient} (built in {@code config.ai.ChatClientConfig}).
 *
 * <p>{@code call().chatResponse()} rather than {@code call().content()}: the full {@link ChatResponse} also carries the provider's
 * metadata (model name, token usage), which the response exposes. No {@code @Transactional}: there is no persistence here, and CLAUDE.md
 * keeps AI/network calls outside transactions anyway.
 */
@Service
class ChatService {

	private final ChatClient chatClient;

	ChatService(final ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	ChatCompletionResponse complete(final ChatCompletionRequest request) {
		final ChatResponse response;
		try {
			response = chatClient.prompt().user(request.message()).call().chatResponse();
		} catch (RuntimeException ex) {
			// Connection refused, provider HTTP error, retries exhausted: all surface as unchecked exceptions from the model call.
			throw new ChatModelUnavailableException("The chat model is unavailable, try again later", ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}

		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new ChatCompletionResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
										  metadata.getModel(),
										  new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}
}
