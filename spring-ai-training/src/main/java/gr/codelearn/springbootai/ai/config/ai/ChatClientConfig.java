package gr.codelearn.springbootai.ai.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Unit 6.2: the one place a {@link ChatClient} is built (CLAUDE.md: "build {@code ChatClient} instances from a central config; do not
 * {@code new} model clients in features"). Features inject {@link ChatClient}; they never see the provider's {@code ChatModel}.
 *
 * <p>{@link ChatClient.Builder} is the prototype bean {@code ChatClientAutoConfiguration} lays over the provider's {@code ChatModel}
 * (Anthropic Claude here, see 6.1). Defaults set on it apply to every call made through the resulting client: {@link SimpleLoggerAdvisor}
 * logs each request and response at DEBUG, which only {@code application-local.yml} switches on.
 */
@Configuration
class ChatClientConfig {

	@Bean
	ChatClient chatClient(final ChatClient.Builder builder) {
		return builder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
	}
}