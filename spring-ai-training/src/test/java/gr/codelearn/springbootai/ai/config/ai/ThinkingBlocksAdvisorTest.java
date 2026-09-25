package gr.codelearn.springbootai.ai.config.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Responses shaped like Spring AI 2.0.1's {@code AnthropicChatModel} builds them when Claude thinks first (default on Claude Sonnet 5): one
 * generation per thinking block, with empty text and a {@code signature}, then the answer. Without {@link ThinkingBlocksAdvisor},
 * {@code getResult()} is the thinking block.
 */
class ThinkingBlocksAdvisorTest {

	private static final Generation THINKING = generation("", Map.of("signature", "sig-1"));
	private static final Generation REDACTED = generation("", Map.of("data", "opaque"));
	private static final Generation ANSWER = generation("{\"city\": \"Athens\"}", Map.of());
	private static final ChatResponseMetadata METADATA = ChatResponseMetadata.builder()
																			 .model("claude-sonnet-5")
																			 .usage(new DefaultUsage(200, 120))
																			 .build();

	private final ChatModel thinkingFirst = new ChatModel() {

		@Override
		public ChatResponse call(final Prompt prompt) {
			return new ChatResponse(List.of(THINKING, REDACTED, ANSWER), METADATA);
		}

		@Override
		public Flux<ChatResponse> stream(final Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(generation(null, Map.of("thinking", true)))),
							 new ChatResponse(List.of(generation(null, Map.of("signature", "sig-1")))),
							 new ChatResponse(List.of(generation("Hello ", Map.of()))),
							 new ChatResponse(List.of(generation("Maria", Map.of()))));
		}
	};

	@Test
	void pitfallWithoutTheAdvisorTheFirstResultIsTheEmptyThinkingBlock() {
		assertThat(ChatClient.create(thinkingFirst).prompt().user("Hi").call().content()).isEmpty();
	}

	@Test
	void theAnswerBecomesTheResultAndTheMetadataStays() {
		final ChatResponse response = client().prompt().user("Hi").call().chatResponse();

		assertThat(response).isNotNull();
		assertThat(response.getResults()).containsExactly(ANSWER);
		assertThat(response.getMetadata().getModel()).isEqualTo("claude-sonnet-5");
		assertThat(response.getMetadata().getUsage().getCompletionTokens()).isEqualTo(120);
	}

	@Test
	void structuredOutputReadsTheAnswerToo() {
		record Place(String city) {
		}

		assertThat(client().prompt().user("Where?").call().entity(Place.class)).isEqualTo(new Place("Athens"));
	}

	@Test
	void streamedThinkingChunksAreSkipped() {
		assertThat(client().prompt().user("Hi").stream().content().collectList().block()).containsExactly("Hello ", "Maria");
	}

	@Test
	void aResponseWithoutThinkingIsLeftAlone() {
		final ChatModel plain = prompt -> new ChatResponse(List.of(ANSWER), METADATA);
		final ChatClient client = ChatClient.builder(plain).defaultAdvisors(new ThinkingBlocksAdvisor()).build();

		assertThat(client.prompt().user("Hi").call().content()).isEqualTo("{\"city\": \"Athens\"}");
	}

	@Test
	void theCentralClientHasTheAdvisor() {
		final ChatClient central = new ChatClientConfig().chatClient(new AiProperties(AiProperties.ChatProvider.ANTHROPIC),
																	 ChatClient.builder(thinkingFirst), thinkingFirst,
																	 new ClassPathResource("prompts/chat-system.st"));

		assertThat(central.prompt().system(SystemPromptParams::fill).user("Hi").call().content()).isEqualTo("{\"city\": \"Athens\"}");
	}

	private ChatClient client() {
		return ChatClient.builder(thinkingFirst).defaultAdvisors(new ThinkingBlocksAdvisor()).build();
	}

	private static Generation generation(final String text, final Map<String, Object> properties) {
		return new Generation(AssistantMessage.builder().content(text).properties(properties).build());
	}
}