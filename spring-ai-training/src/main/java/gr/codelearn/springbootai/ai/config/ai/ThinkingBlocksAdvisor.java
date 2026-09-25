package gr.codelearn.springbootai.ai.config.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Drops the separate thinking-block generations Anthropic's model adds to a response, so {@code ChatResponse.getResult()} is the answer.
 *
 * <p>Claude Sonnet 5 thinks by default (adaptive thinking runs when the request sets no {@code thinking} parameter), and its thinking text
 * is omitted by default, so a thinking block arrives with empty text. Spring AI 2.0.1's {@code AnthropicChatModel} turns each thinking
 * block into its own {@link Generation}, <em>before</em> the answer's generation. Everything that reads {@code getResult()} (every service
 * here, {@code .content()}, {@code .entity(...)}, the structured output validation advisor) then gets the thinking block: an empty reply,
 * or "Empty JSON output" for structured output.
 *
 * <p>Nothing is lost by dropping them: the answer's generation carries the thinking blocks too (as properties, for replaying the turn to
 * the same model), and the response metadata (model, token usage) stays as it was. A thinking generation is recognised by the markers
 * {@code AnthropicChatModel} sets on it: {@code signature} (a thinking block), {@code data} (a redacted one), or {@code thinking = true} (a
 * streamed thinking chunk). Other providers never set them (Ollama puts its thinking in the answer's own metadata), so the advisor is a
 * no-op there.
 *
 * <p>It runs just outside the model call ({@link Ordered#LOWEST_PRECEDENCE} - 1), so every other advisor already sees the cleaned
 * response.
 */
class ThinkingBlocksAdvisor implements CallAdvisor, StreamAdvisor {

	static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;

	private static final Logger log = LoggerFactory.getLogger(ThinkingBlocksAdvisor.class);

	@Override
	public String getName() {
		return "ThinkingBlocksAdvisor";
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public ChatClientResponse adviseCall(final ChatClientRequest request, final CallAdvisorChain chain) {
		final ChatClientResponse response = chain.nextCall(request);
		final ChatResponse chatResponse = response.chatResponse();
		if (chatResponse == null || chatResponse.getResults().stream().noneMatch(ThinkingBlocksAdvisor::isThinking)) {
			return response;
		}
		final ChatResponse answersOnly = withoutThinking(chatResponse);
		log.debug("Dropped {} thinking block generation(s)", chatResponse.getResults().size() - answersOnly.getResults().size());
		return response.mutate().chatResponse(answersOnly).build();
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(final ChatClientRequest request, final StreamAdvisorChain chain) {
		// A streamed thinking chunk is a response of its own with only a thinking generation: skip it altogether.
		return chain.nextStream(request).filter(response -> {
			final ChatResponse chatResponse = response.chatResponse();
			return chatResponse == null || chatResponse.getResults().isEmpty()
				   || !chatResponse.getResults().stream().allMatch(ThinkingBlocksAdvisor::isThinking);
		});
	}

	static boolean isThinking(final Generation generation) {
		final Map<String, Object> metadata = generation.getOutput().getMetadata();
		return metadata.containsKey("signature") || metadata.containsKey("data") || Boolean.TRUE.equals(metadata.get("thinking"));
	}

	private static ChatResponse withoutThinking(final ChatResponse response) {
		final List<Generation> answers = response.getResults().stream().filter(generation -> !isThinking(generation)).toList();
		return new ChatResponse(answers, response.getMetadata());
	}
}