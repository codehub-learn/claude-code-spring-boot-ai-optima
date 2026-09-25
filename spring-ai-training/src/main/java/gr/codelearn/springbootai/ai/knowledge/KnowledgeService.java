package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.config.ai.SystemPromptParams;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerRequest;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse.Source;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeAnswerResponse.TokenUsage;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unit 10.1: step 2 of RAG, answering: the reference flow.
 *
 * <ol>
 *   <li><b>Query</b>: the caller's question, as the user message.</li>
 *   <li><b>Retrieval</b>: {@link QuestionAnswerAdvisor} embeds it (same model as the documents) and asks Oracle for the nearest
 *   documents ({@code VECTOR_DISTANCE(embedding, ?, COSINE)}, top-k).</li>
 *   <li><b>Context</b>: the advisor renders {@code prompts/knowledge/question-answer.st}: instructions, the documents' text, the question
 *   last; that replaces the user message. The system prompt stays first, as on every call.</li>
 *   <li><b>Generation</b>: the chat model answers from that context and cites the tags; the advisor adds the retrieved documents to the
 *   response metadata, returned here as {@code sources}.</li>
 * </ol>
 *
 * <p>Nothing in this class searches or builds the context: it adds the advisor for this call only (see {@code config.ai.RagConfig}).
 * Unit 10.2: with a filter, passed per call ({@link QuestionAnswerAdvisor#FILTER_EXPRESSION}), so retrieval only sees the active chunking
 * strategy's chunks and the catalog rows ({@code app.ai.knowledge.chunking.active}).
 */
@Service
class KnowledgeService {

	private static final JsonMapper JSON = JsonMapper.shared();

	private static final String UNAVAILABLE = "The chat or embedding model is unavailable, try again later";

	private final ChatClient chatClient;
	private final QuestionAnswerAdvisor questionAnswerAdvisor;
	private final KnowledgeProperties properties;

	KnowledgeService(final ChatClient chatClient, final QuestionAnswerAdvisor questionAnswerAdvisor,
					 final KnowledgeProperties properties) {
		this.chatClient = chatClient;
		this.questionAnswerAdvisor = questionAnswerAdvisor;
		this.properties = properties;
	}

	KnowledgeAnswerResponse answer(final KnowledgeAnswerRequest request) {
		final ChatResponse response;
		try {
			response = chatClient.prompt()
								 .system(SystemPromptParams::fill)
								 .user(request.question())
								 .advisors(advisors -> advisors.advisors(questionAnswerAdvisor)
															   .param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
																	  properties.chunking().active().filterExpression()))
								 .call()
								 .chatResponse();
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException(UNAVAILABLE, ex);
		}
		if (response == null || response.getResult() == null) {
			throw new ChatModelUnavailableException("The chat model returned no result", null);
		}
		final ChatResponseMetadata metadata = response.getMetadata();
		final Usage usage = metadata.getUsage();
		return new KnowledgeAnswerResponse(Objects.requireNonNullElse(response.getResult().getOutput().getText(), ""),
										   sources(metadata), metadata.getModel(),
										   new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens()));
	}

	private static List<Source> sources(final ChatResponseMetadata metadata) {
		final Object retrieved = metadata.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
		if (!(retrieved instanceof List<?> documents)) {
			return List.of();
		}
		return documents.stream().filter(Document.class::isInstance).map(Document.class::cast).map(KnowledgeService::toSource).toList();
	}

	private static Source toSource(final Document document) {
		final Map<String, Object> metadata = document.getMetadata();
		final Object section = metadata.get("section");
		return new Source(text(metadata.get("source")), text(metadata.get("ref")), text(metadata.get("title")),
						  section == null ? null : text(section), document.getScore());
	}

	/**
	 * A metadata value as plain text. OracleVectorStore (2.0.1) hands back the driver's JSON values ({@code OracleJsonString}), whose
	 * {@code toString()} is the JSON text: {@code "LAP-0002"} with the quotes. Parsed as JSON, it is the string again.
	 */
	static String text(final @Nullable Object value) {
		if (value == null || value instanceof String) {
			return Objects.requireNonNullElse((String) value, "");
		}
		final String json = value.toString();
		return json.startsWith("\"") ? JSON.readValue(json, String.class) : json;
	}
}