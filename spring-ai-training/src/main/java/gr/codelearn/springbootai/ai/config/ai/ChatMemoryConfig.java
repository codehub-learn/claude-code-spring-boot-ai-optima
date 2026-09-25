package gr.codelearn.springbootai.ai.config.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Unit 9.2: conversation memory, the Spring AI way. Three layers, each replaceable on its own:
 *
 * <ul>
 *   <li>{@link ChatMemoryRepository}: <em>where</em> messages are stored. Not declared here: the JDBC starter's autoconfig provides a
 *   {@code JdbcChatMemoryRepository} (Oracle dialect picked from the {@code DataSource}), ahead of the in-memory default. Its table comes
 *   from Flyway ({@code V1__create_chat_memory.sql}).</li>
 *   <li>{@link ChatMemory}: <em>which</em> messages are kept. {@link MessageWindowChatMemory} keeps the last
 *   {@link ChatMemoryProperties#maxMessages()} and deletes the rest when a turn is saved (eviction is by count, never by age). The window
 *   always starts at a user message, so a reply is never kept without its question.</li>
 *   <li>{@link MessageChatMemoryAdvisor}: <em>when</em> memory is used. Before the call it puts the stored messages in front of the new
 *   one and saves the new user message; after the call it saves the reply.</li>
 * </ul>
 *
 * <p>The advisor is a bean, not a default advisor of the central {@code ChatClient}: it needs a conversation id on every call
 * ({@code ChatMemory.CONVERSATION_ID}, and fails without one), and the stateless endpoints have none. {@code ConversationService} adds it
 * per call with the id. Its order ({@code DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}) is outside the logger and {@link ThinkingBlocksAdvisor},
 * so the logger shows the prompt with the history in it and memory stores the answer, never a thinking block.
 */
@Configuration
class ChatMemoryConfig {

	private static final Logger log = LoggerFactory.getLogger(ChatMemoryConfig.class);

	@Bean
	ChatMemory chatMemory(final ChatMemoryRepository chatMemoryRepository, final ChatMemoryProperties properties) {
		log.info("Chat memory: repository={}, window={} messages", chatMemoryRepository.getClass().getSimpleName(),
				 properties.maxMessages());
		return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).maxMessages(properties.maxMessages()).build();
	}

	@Bean
	MessageChatMemoryAdvisor chatMemoryAdvisor(final ChatMemory chatMemory) {
		return MessageChatMemoryAdvisor.builder(chatMemory).build();
	}
}