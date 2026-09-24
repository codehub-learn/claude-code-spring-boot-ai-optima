package gr.codelearn.springbootai.ai;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 6.1: the Spring AI layering, {@code ChatClient} (fluent API) → {@code Advisor} chain → {@code ChatModel} (provider SPI), proven with
 * a stub {@link ChatModel} so no provider starter or API key is needed. {@link ChatClientAutoConfiguration} is the same auto-configuration
 * every {@code spring-ai-starter-model-*} brings in.
 */
class SpringAiArchitectureTest {

	private static final String REPLY = "stub reply";

	private final List<String> trace = new ArrayList<>();
	private final List<Prompt> prompts = new ArrayList<>();

	/**
	 * Stands in for a provider implementation (Anthropic, Ollama, ...): records the Prompt it receives, returns a fixed reply.
	 */
	private final ChatModel stubModel = prompt -> {
		trace.add("model");
		prompts.add(prompt);
		return new ChatResponse(List.of(new Generation(new AssistantMessage(REPLY))));
	};

	private final ApplicationContextRunner runner =
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ChatClientAutoConfiguration.class));

	@Test
	void chatClientBuilderIsAutoConfiguredOnTopOfTheChatModel() {
		runner.withBean(ChatModel.class, () -> stubModel)
			  .run(context -> assertThat(context).hasSingleBean(ChatClient.Builder.class));
	}

	@Test
	void callFlowsThroughTheAdvisorChainDownToTheChatModel() {
		runner.withBean(ChatModel.class, () -> stubModel).run(context -> {
			ChatClient chatClient = context.getBean(ChatClient.Builder.class)
										   .defaultAdvisors(new TracingAdvisor(trace), new SimpleLoggerAdvisor())
										   .build();

			String content = chatClient.prompt().system("You are a terse assistant.").user("Say hi.").call().content();

			assertThat(content).isEqualTo(REPLY);
			assertThat(trace).containsExactly("advisor:before", "model", "advisor:after");
			assertThat(prompts).singleElement()
							   .satisfies(prompt -> assertThat(prompt.getInstructions()).satisfiesExactly(
									   system -> assertThat(system).isInstanceOf(SystemMessage.class)
																   .extracting(m -> m.getText())
																   .isEqualTo("You are a terse assistant."),
									   user -> assertThat(user).isInstanceOf(UserMessage.class)
															   .extracting(m -> m.getText())
															   .isEqualTo("Say hi.")));
		});
	}

	@Test
	void builderIsRegisteredButUnusableWithoutAChatModel() {
		// Prototype-scoped and not guarded by @ConditionalOnBean(ChatModel): the context starts, the first injection fails.
		runner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThatThrownBy(() -> context.getBean(ChatClient.Builder.class)).isInstanceOf(UnsatisfiedDependencyException.class);
		});
	}

	@Test
	void builderCanBeSwitchedOffByProperty() {
		runner.withBean(ChatModel.class, () -> stubModel)
			  .withPropertyValues("spring.ai.chat.client.enabled=false")
			  .run(context -> assertThat(context).doesNotHaveBean(ChatClient.Builder.class));
	}

	/**
	 * A minimal around-advice: code before {@code nextCall} sees the request, code after it sees the response.
	 */
	private record TracingAdvisor(List<String> trace) implements CallAdvisor {

		@Override
		public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
			trace.add("advisor:before");
			ChatClientResponse response = chain.nextCall(request);
			trace.add("advisor:after");
			return response;
		}

		@Override
		public String getName() {
			return "tracing";
		}

		@Override
		public int getOrder() {
			return 0;
		}
	}
}