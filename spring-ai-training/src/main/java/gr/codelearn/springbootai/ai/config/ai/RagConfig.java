package gr.codelearn.springbootai.ai.config.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Unit 10.1: the RAG wiring. The pieces come from autoconfiguration: the {@link EmbeddingModel} is Ollama's
 * ({@code spring.ai.model.embedding: ollama}, whichever chat provider is active) and the {@link VectorStore} is Oracle AI Vector Search
 * ({@code spring.ai.vectorstore.oracle.*}, table {@code knowledge_vector} from Flyway). This class adds the step that joins them to a chat
 * call: a {@link QuestionAnswerAdvisor}, which before the call
 *
 * <ol>
 *   <li>embeds the user message and searches the store for the {@link RetrievalProperties#topK()} nearest documents,</li>
 *   <li>renders {@code prompts/knowledge/question-answer.st} with their text ({@code {question_answer_context}}) and the question
 *   ({@code {query}}), and sends that as the user message,</li>
 * </ol>
 *
 * <p>and after the call puts the retrieved documents in the response metadata ({@link QuestionAnswerAdvisor#RETRIEVED_DOCUMENTS}), so the
 * caller can show its sources. Only each document's <em>text</em> reaches the model, never its metadata: whatever the model must cite
 * has to
 * be written into the text at ingestion.
 *
 * <p>A bean, not a default advisor of the central {@code ChatClient}: every call would then search the store and answer only from it.
 * {@code knowledge.KnowledgeService} adds it per call. Its order is set explicitly: the default (0) ties with {@code SimpleLoggerAdvisor},
 * and this one must run first so the debug log shows the augmented prompt. It still runs after the chat memory advisor.
 */
@Configuration
class RagConfig {

	static final int ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100;

	private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

	@Bean
	QuestionAnswerAdvisor questionAnswerAdvisor(final VectorStore vectorStore, final RetrievalProperties properties,
												@Value("classpath:prompts/knowledge/question-answer.st") final Resource template) {
		log.info("Retrieval: store={}, top-k={}, similarity-threshold={}", vectorStore.getName(), properties.topK(),
				 properties.similarityThreshold());
		final SearchRequest searchRequest =
				SearchRequest.builder().topK(properties.topK()).similarityThreshold(properties.similarityThreshold()).build();
		return QuestionAnswerAdvisor.builder(vectorStore)
									.searchRequest(searchRequest)
									.promptTemplate(new PromptTemplate(template))
									.order(ORDER)
									.build();
	}
}