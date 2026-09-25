package gr.codelearn.springbootai.ai.config.ai;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 7.4: the application's own switch between the cloud and the local chat model, bound from {@code app.ai.*}.
 *
 * <p>{@code application.yml} feeds it into Spring AI's selector, {@code spring.ai.model.chat: ${app.ai.chat-provider}}, so this is the
 * one key to set; the profiles do exactly that ({@code local} → Ollama, {@code anthropic} → Anthropic, no profile → Anthropic). Binding it
 * to an enum turns a typo into a readable startup error naming the valid values, instead of the {@code NoSuchBeanDefinitionException} for
 * {@code ChatModel} that a wrong {@code spring.ai.model.chat} ends in (7.1). {@link ChatClientConfig} takes it as its first dependency, so
 * it is bound and validated before Spring looks for a {@code ChatModel}.
 */
@Validated
@ConfigurationProperties("app.ai")
record AiProperties(@NotNull ChatProvider chatProvider) {

	/**
	 * The chat providers whose starters are on the classpath. Each name is the value Spring AI's {@code spring.ai.model.chat} expects.
	 */
	enum ChatProvider {
		/**
		 * Anthropic Claude, in the cloud.
		 */
		ANTHROPIC,
		/**
		 * A local Ollama server (7.3).
		 */
		OLLAMA
	}
}