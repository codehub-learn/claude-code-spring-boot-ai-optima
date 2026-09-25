package gr.codelearn.springbootai.ai.answer;

import gr.codelearn.springbootai.ai.answer.dto.AnswerRequest;
import gr.codelearn.springbootai.ai.answer.dto.AnswerResponse;
import gr.codelearn.springbootai.ai.answer.dto.AnswerResponse.TokenUsage;
import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Unit 8.2: answers a question from the caller's documents, or from the model's own knowledge when there are none. The rendered prompt
 * ({@link AnswerPrompts}) is the user message; the application's system prompt (6.4) still comes first and is not touched, so the standing
 * rules and the task's instructions stay separate.
 *
 * <p>No {@code @Transactional}: nothing is persisted, and CLAUDE.md keeps AI calls outside transactions anyway.
 */
@Service
class AnswerService {

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;
	private final AnswerPrompts prompts;

	AnswerService(final ChatClient chatClient, final AnswerPrompts prompts) {
		this.chatClient = chatClient;
		this.prompts = prompts;
	}

	AnswerResponse answer(final AnswerRequest request) {
		final String userMessage = prompts.question(request);
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
		return new AnswerResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
								  metadata.getModel(),
								  new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}
}