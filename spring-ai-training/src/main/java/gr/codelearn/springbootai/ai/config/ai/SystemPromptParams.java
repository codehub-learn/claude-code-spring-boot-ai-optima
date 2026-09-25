package gr.codelearn.springbootai.ai.config.ai;

import org.springframework.ai.chat.client.ChatClient.PromptSystemSpec;

import java.time.LocalDate;

/**
 * Fills the placeholders of the default system prompt ({@code prompts/chat-system.st}, set in {@link ChatClientConfig}) while keeping its
 * text. Every call through the central {@code ChatClient} passes it: {@code chatClient.prompt().system(SystemPromptParams::fill)}. Without
 * any parameter the template is not rendered at all, and the model would read a literal {@code {today}} (6.4).
 *
 * <p>It lives next to the template's wiring, not in a feature, because every feature that calls the model needs it (moved out of
 * {@code chat.ChatService} in 8.1).
 */
public final class SystemPromptParams {

	private SystemPromptParams() {
	}

	public static void fill(final PromptSystemSpec system) {
		system.param("today", LocalDate.now());
	}
}