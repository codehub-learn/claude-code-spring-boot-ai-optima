package gr.codelearn.springbootai.ai.config.ai;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Unit 9.2: the chat memory's window, bound from {@code app.ai.memory.*}. Spring AI's {@code ChatMemoryAutoConfiguration} builds a
 * {@code MessageWindowChatMemory} with a hard-coded 20 messages and no property to change it, so {@link ChatMemoryConfig} builds its own
 * from this.
 *
 * @param maxMessages how many messages (user and assistant, 2 per turn) one conversation's memory keeps; older ones are deleted when a new
 *                    turn is saved. At least 2, one complete turn.
 */
@Validated
@ConfigurationProperties("app.ai.memory")
record ChatMemoryProperties(@DefaultValue("20") @Min(2) int maxMessages) {
}