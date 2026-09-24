package gr.codelearn.springbootai.ai.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Unit 6.2: the one place a {@link ChatClient} is built (CLAUDE.md: "build {@code ChatClient} instances from a central config; do not
 * {@code new} model clients in features"). Features inject {@link ChatClient}; they never see the provider's {@code ChatModel}.
 *
 * <p>{@link ChatClient.Builder} is the prototype bean {@code ChatClientAutoConfiguration} lays over the provider's {@code ChatModel}
 * (Anthropic Claude here, see 6.1). Defaults set on it apply to every call made through the resulting client: {@link SimpleLoggerAdvisor}
 * logs each request and response at DEBUG, which only {@code application-local.yml} switches on.
 *
 * <p>Unit 6.4: the default system prompt is the {@code prompts/chat-system.st} template, loaded as a {@link Resource} so the instructions
 * live next to the code as a reviewable text file, not as a Java string. Every call gets it as the first message. Its one placeholder,
 * {@code {today}}, has no default here: it changes per call, so each call site fills it with {@code .system(s -> s.param("today", ...))}.
 * Nothing checks this at startup. A call that passes no parameters at all skips rendering and sends {@code {today}} to the model as literal
 * text; a call that passes some parameters but not {@code today} fails with an {@code IllegalStateException}. Two ways to lose these rules
 * by accident:
 *
 * <ul>
 *   <li>a per-call {@code .system(String)} or {@code .system(Resource)} <em>replaces</em> the default text entirely;</li>
 *   <li>a {@code SystemMessage} passed inside {@code prompt(Prompt)} or {@code .messages(...)} is <em>added</em> after it, so a second set
 *   of system instructions competes with the application's. That is why {@code /api/v1/prompts} does not accept a {@code system} role.</li>
 * </ul>
 */
@Configuration
class ChatClientConfig {

	@Bean
	ChatClient chatClient(final ChatClient.Builder builder,
						  @Value("classpath:prompts/chat-system.st") final Resource chatSystemPrompt) {
		return builder.defaultSystem(chatSystemPrompt).defaultAdvisors(new SimpleLoggerAdvisor()).build();
	}
}