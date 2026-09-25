package gr.codelearn.springbootai.ai.writing;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.writing.dto.ProductDescriptionRequest;
import gr.codelearn.springbootai.ai.writing.dto.SummaryRequest;
import gr.codelearn.springbootai.ai.writing.dto.WritingResponse;
import gr.codelearn.springbootai.ai.writing.dto.WritingResponse.TokenUsage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Unit 8.1: each use case renders its own template ({@link WritingPrompts}) into the user message and sends it through the central
 * {@link ChatClient}. The application's system prompt still applies (its {@code {today}} is filled by {@link SystemPromptParams}): the
 * template is the task, the system prompt the standing rules.
 *
 * <p>The rendered text goes in with {@code .user(String)}, which ChatClient never templates again (6.4), so a user's text containing
 * braces is sent as is. No {@code @Transactional}: nothing is persisted, and CLAUDE.md keeps AI calls outside transactions anyway.
 */
@Service
class WritingService {

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;
	private final WritingPrompts prompts;

	WritingService(final ChatClient chatClient, final WritingPrompts prompts) {
		this.chatClient = chatClient;
		this.prompts = prompts;
	}

	WritingResponse summarize(final SummaryRequest request) {
		return write(prompts.summary(request));
	}

	WritingResponse describeProduct(final ProductDescriptionRequest request) {
		return write(prompts.productDescription(request));
	}

	private WritingResponse write(final String userMessage) {
		final ChatResponse response;
		try {
			response = chatClient.prompt().system(SystemPromptParams::fill).user(userMessage).call().chatResponse();
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new WritingResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
								   metadata.getModel(),
								   new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}
}