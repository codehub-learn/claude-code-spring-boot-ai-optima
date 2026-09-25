package gr.codelearn.springbootai.ai.conversation;

import org.springframework.data.repository.ListCrudRepository;

/**
 * Unit 9.2: the conversation registry. Deleting a row also deletes the conversation's chat memory: the memory table's foreign key is
 * {@code on delete cascade} ({@code V1__create_chat_memory.sql}).
 */
interface ConversationRepository extends ListCrudRepository<Conversation, String> {
}