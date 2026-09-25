package gr.codelearn.springbootai.ai.conversation;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageRequest;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationMessageResponse.Role;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationReplyResponse.TokenUsage;
import gr.codelearn.springbootai.ai.conversation.dto.ConversationResponse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Unit 9.1: a stateful multi-turn conversation, built by hand. A model is stateless: every call must carry everything it should know.
 * "Memory" is the application sending the earlier turns again, so each turn is:
 *
 * <ol>
 *   <li>load the conversation's history (user, assistant, user, ...);</li>
 *   <li>call the model with the system prompt, that history ({@code .messages(...)}) and the new user message;</li>
 *   <li>store the new user message and the reply as one turn, only if the call succeeded.</li>
 * </ol>
 *
 * <p>The whole turn runs under the conversation's lock (see {@link Conversation}). At most {@value #MAX_TURNS} turns per conversation:
 * the history grows the prompt every turn and would eventually overflow the model's context window; 9.2 replaces the hard stop with a
 * sliding window. No {@code @Transactional}: the state is in memory, and AI calls stay outside transactions anyway.
 */
@Service
class ConversationService {

	static final int MAX_TURNS = 20;

	private static final String UNAVAILABLE = "The chat model is unavailable, try again later";

	private final ChatClient chatClient;
	private final ConversationStore store;

	ConversationService(final ChatClient chatClient, final ConversationStore store) {
		this.chatClient = chatClient;
		this.store = store;
	}

	ConversationResponse create() {
		final Conversation conversation = store.create();
		return new ConversationResponse(conversation.id(), conversation.createdAt());
	}

	ConversationReplyResponse send(final UUID id, final ConversationMessageRequest request) {
		final Conversation conversation = find(id);
		conversation.lock().lock();
		try {
			final List<Message> history = conversation.messages();
			if (history.size() / 2 >= MAX_TURNS) {
				throw new ConversationLimitException("The conversation reached " + MAX_TURNS + " turns, start a new one");
			}
			final UserMessage userMessage = new UserMessage(request.content());
			final ChatResponse response = call(history, userMessage);
			final AssistantMessage reply = response.getResult().getOutput();
			conversation.addTurn(userMessage, reply);
			return toReply(conversation, response);
		} finally {
			conversation.lock().unlock();
		}
	}

	List<ConversationMessageResponse> history(final UUID id) {
		return find(id).messages().stream().map(ConversationService::toMessageResponse).toList();
	}

	void delete(final UUID id) {
		if (!store.delete(id)) {
			throw new ConversationNotFoundException(id);
		}
	}

	private Conversation find(final UUID id) {
		return store.find(id).orElseThrow(() -> new ConversationNotFoundException(id));
	}

	private ChatResponse call(final List<Message> history, final UserMessage userMessage) {
		final ChatResponse response;
		try {
			response = chatClient.prompt()
								 .system(SystemPromptParams::fill)
								 .messages(history)
								 .messages(userMessage)
								 .call()
								 .chatResponse();
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		return response;
	}

	private static ConversationReplyResponse toReply(final Conversation conversation, final ChatResponse response) {
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new ConversationReplyResponse(conversation.id(), conversation.turns(),
											 Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
											 metadata.getModel(),
											 new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	private static ConversationMessageResponse toMessageResponse(final Message message) {
		final Role role = message instanceof AssistantMessage ? Role.ASSISTANT : Role.USER;
		return new ConversationMessageResponse(role, Objects.requireNonNullElse(message.getText(), ""));
	}
}