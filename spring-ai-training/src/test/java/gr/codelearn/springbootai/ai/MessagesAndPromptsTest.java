package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 6.3: the message model and the two ways to get an answer, proven with a stub {@link ChatModel} (no provider, no API key).
 *
 * <ul>
 *   <li>the four {@link Message} types and the {@link MessageType} (role) each one carries;</li>
 *   <li>how a tool round trip is recorded: an {@link AssistantMessage} carrying {@link ToolCall}s, answered by a
 *   {@link ToolResponseMessage}. Built by hand here; U11 lets Spring AI run real {@code @Tool} methods;</li>
 *   <li>{@link Prompt} construction: a bare string, an explicit message list, per-call {@link ChatOptions};</li>
 *   <li>blocking {@code call()} (one {@link ChatResponse} when the model is done) vs streaming {@code stream()} (a {@link Flux} of partial
 *   {@link ChatResponse}s as the model generates).</li>
 * </ul>
 */
class MessagesAndPromptsTest {

	private final List<Prompt> prompts = new ArrayList<>();

	/**
	 * {@code call} answers with the whole reply at once; {@code stream} emits the same reply as three fragments, the way a provider
	 * streams
	 * tokens. {@link ChatModel} extends {@code StreamingChatModel}, whose default {@code stream} throws, so a streaming stub must override
	 * it.
	 */
	private final ChatModel stubModel = new ChatModel() {

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			return reply("Hello there!");
		}

		@Override
		public Flux<ChatResponse> stream(final Prompt prompt) {
			prompts.add(prompt);
			return Flux.just("Hello", " there", "!").map(MessagesAndPromptsTest::reply);
		}
	};

	private final ChatClient chatClient = ChatClient.create(stubModel);

	@Test
	void everyMessageTypeCarriesItsRole() {
		final List<Message> messages = List.of(new SystemMessage("You are terse."), new UserMessage("Hi"), new AssistantMessage("Hello!"),
											   ToolResponseMessage.builder()
																  .responses(
																		  List.of(new ToolResponse("call-1", "currentTime", "\"10:15\"")))
																  .build());

		assertThat(messages).extracting(Message::getMessageType)
							.containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL);
		assertThat(messages).extracting(Message::getMessageType)
							.extracting(MessageType::getValue)
							.containsExactly("system", "user", "assistant", "tool");
	}

	@Test
	void aToolRoundTripIsAnAssistantToolCallAnsweredByAToolResponse() {
		// 1. the user asks; 2. the model does not answer, it asks the application to run a tool; 3. the application sends the tool's
		// result
		// back; 4. the model then writes the final answer (next call). ToolCall.id and ToolResponse.id pair the request with its result.
		final AssistantMessage toolRequest = AssistantMessage.builder()
															 .content("")
															 .toolCalls(List.of(new ToolCall("call-1", "function", "currentTime",
																							 "{\"zone\":\"Europe/Athens\"}")))
															 .build();
		final ToolResponseMessage toolResult = ToolResponseMessage.builder()
																  .responses(
																		  List.of(new ToolResponse("call-1", "currentTime", "\"10:15\"")))
																  .build();
		final Prompt prompt = new Prompt(List.of(new UserMessage("What time is it in Athens?"), toolRequest, toolResult));

		assertThat(toolRequest.hasToolCalls()).isTrue();
		assertThat(toolRequest.getToolCalls().getFirst().id()).isEqualTo(toolResult.getResponses().getFirst().id());
		// What the model is answering now is the tool result, not the original question.
		assertThat(prompt.getLastUserOrToolResponseMessage()).isSameAs(toolResult);
	}

	@Test
	void aStringPromptIsOneUserMessage() {
		final Prompt prompt = new Prompt("Hi");

		assertThat(prompt.getInstructions()).singleElement().isInstanceOf(UserMessage.class);
		assertThat(prompt.getUserMessage().getText()).isEqualTo("Hi");
		assertThat(prompt.getSystemMessage().getText()).as("no system message: an empty placeholder, never null").isEmpty();
	}

	@Test
	void anExplicitPromptKeepsMessageOrderAndPerCallOptions() {
		final ChatOptions options = ChatOptions.builder().temperature(0.0).maxTokens(50).build();
		final Prompt prompt = new Prompt(List.of(new SystemMessage("Answer in one word."), new UserMessage("Capital of Greece?"),
												 new AssistantMessage("Athens."), new UserMessage("And of Italy?")), options);

		final String reply = chatClient.prompt(prompt).call().content();

		assertThat(reply).isEqualTo("Hello there!");
		assertThat(prompts).singleElement().satisfies(received -> {
			assertThat(received.getInstructions()).extracting(Message::getMessageType)
												  .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT,
																   MessageType.USER);
			// Earlier turns stay in the prompt; the one the model answers is the last user message.
			assertThat(received.getUserMessage().getText()).isEqualTo("And of Italy?");
			assertThat(received.getOptions().getTemperature()).isEqualTo(0.0);
			assertThat(received.getOptions().getMaxTokens()).isEqualTo(50);
		});
	}

	@Test
	void theFluentApiBuildsTheSamePrompt() {
		chatClient.prompt().system("Answer in one word.").user("Capital of Greece?").call().content();

		assertThat(prompts).singleElement()
						   .satisfies(received -> assertThat(received.getInstructions()).satisfiesExactly(
								   system -> assertThat(system).isInstanceOf(SystemMessage.class),
								   user -> assertThat(user).isInstanceOf(UserMessage.class)));
	}

	@Test
	void callBlocksForTheWholeReplyWhileStreamEmitsFragments() {
		final ChatResponse blocking = chatClient.prompt().user("Greet me").call().chatResponse();
		final List<String> fragments = chatClient.prompt().user("Greet me").stream().content().collectList().block();

		assertThat(blocking).isNotNull();
		assertThat(blocking.getResult().getOutput().getText()).isEqualTo("Hello there!");
		assertThat(fragments).containsExactly("Hello", " there", "!");
		assertThat(String.join("", fragments)).isEqualTo(blocking.getResult().getOutput().getText());
	}

	@Test
	void messagesCarryMetadataBesideTheText() {
		final UserMessage message = UserMessage.builder().text("Hi").metadata(Map.of("source", "web")).build();

		assertThat(message.getText()).isEqualTo("Hi");
		assertThat(message.getMetadata()).containsEntry("source", "web");
	}

	private static ChatResponse reply(final String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}
}