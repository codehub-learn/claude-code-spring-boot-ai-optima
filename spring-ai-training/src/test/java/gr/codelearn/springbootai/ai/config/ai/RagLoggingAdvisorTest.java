package gr.codelearn.springbootai.ai.config.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit 10.3: what {@link RagLoggingAdvisor} logs around a real {@link QuestionAnswerAdvisor} call (in-memory store, toy embeddings, stub
 * chat model): the stage events and their key-value pairs at DEBUG, nothing at INFO, and never the question, a chunk's text or the answer.
 */
class RagLoggingAdvisorTest {

	private static final String QUESTION = "How long is the laptop warranty?";
	private static final String CHUNK_TEXT = "[doc:warranty] Warranty > Laptops: laptops carry a 2-year warranty.";
	private static final String ANSWER = "Two years [doc:warranty].";

	private final Logger logger = (Logger) LoggerFactory.getLogger(RagLoggingAdvisor.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private final SimpleVectorStore vectorStore = SimpleVectorStore.builder(new ToyEmbeddingModel()).build();
	private boolean failing;

	private final ChatModel chatModel = new ChatModel() {

		@Override
		public ChatResponse call(final Prompt prompt) {
			if (failing) {
				throw new IllegalStateException("model down");
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage(ANSWER))),
									ChatResponseMetadata.builder().model("stub-model").usage(new DefaultUsage(300, 12)).build());
		}
	};

	@BeforeEach
	void attachAppender() {
		appender.start();
		logger.addAppender(appender);
		vectorStore.add(List.of(chunk("laptops", CHUNK_TEXT, 0, 3), chunk("returns", "[doc:returns] Returns: 14 days.", 1, 5)));
	}

	@AfterEach
	void detachAppender() {
		logger.detachAppender(appender);
		logger.setLevel(null);
	}

	@Test
	void atDebugEachStageIsOneStructuredEvent() {
		logger.setLevel(Level.DEBUG);

		ask();

		assertThat(appender.list).extracting(event -> keyValues(event).get("rag.stage"))
								 .containsExactly("retrieval", "hit", "hit", "generation");
		assertThat(keyValues(appender.list.getFirst())).containsEntry("rag.hits", 2).containsKeys("rag.top_similarity",
																								  "rag.context_tokens");
		assertThat(keyValues(appender.list.get(1))).containsEntry("rag.rank", 1)
												   .containsEntry("rag.ref", "warranty")
												   .containsEntry("rag.section", "laptops")
												   .containsEntry("rag.chunk", "markdown 1/3");
		assertThat(keyValues(appender.list.getLast())).containsEntry("rag.prompt_tokens", 300)
													  .containsEntry("rag.completion_tokens", 12)
													  .containsEntry("rag.answer_chars", ANSWER.length())
													  .containsEntry("rag.model", "stub-model");
		// The same values are in the message, for the plain-text local console.
		assertThat(appender.list.get(1).getFormattedMessage()).isEqualTo(
				"RAG hit 1: doc:warranty > laptops [markdown 1/3], similarity 1.00, "
				+ keyValues(appender.list.get(1)).get("rag.tokens") + " tokens");
	}

	@Test
	void neverLogsTheQuestionTheChunkTextOrTheAnswer() {
		logger.setLevel(Level.DEBUG);

		ask();

		assertThat(appender.list).isNotEmpty().allSatisfy(event -> {
			final String logged = event.getFormattedMessage() + keyValues(event).values();
			assertThat(logged).doesNotContain(QUESTION).doesNotContain("2-year warranty").doesNotContain(ANSWER);
		});
	}

	@Test
	void atInfoNothingIsLogged() {
		logger.setLevel(Level.INFO);

		assertThat(ask()).isEqualTo(ANSWER);

		assertThat(appender.list).isEmpty();
	}

	@Test
	void noHitIsLoggedAsAnEmptyRetrieval() {
		logger.setLevel(Level.DEBUG);
		vectorStore.delete(List.of("laptops", "returns"));

		ask();

		assertThat(appender.list.getFirst().getFormattedMessage()).startsWith("RAG retrieval: no document retrieved");
		assertThat(keyValues(appender.list.getFirst())).containsEntry("rag.hits", 0);
		assertThat(appender.list).hasSize(2);
	}

	@Test
	void aFailedGenerationIsLoggedAfterTheRetrievalAndRethrown() {
		logger.setLevel(Level.DEBUG);
		failing = true;

		assertThatThrownBy(this::ask).isInstanceOf(IllegalStateException.class).hasMessage("model down");

		assertThat(appender.list).extracting(event -> keyValues(event).get("rag.stage"))
								 .containsExactly("retrieval", "hit", "hit", "generation-failed");
		assertThat(keyValues(appender.list.getLast())).containsEntry("rag.error", "IllegalStateException");
	}

	@Test
	void itRunsJustInsideTheRetrievalAdvisor() {
		assertThat(new RagLoggingAdvisor().getOrder()).isEqualTo(RagConfig.ORDER + 1);
	}

	private String ask() {
		final QuestionAnswerAdvisor retrieval =
				QuestionAnswerAdvisor.builder(vectorStore)
									 .searchRequest(SearchRequest.builder().topK(2).build())
									 .order(RagConfig.ORDER)
									 .build();
		return ChatClient.builder(chatModel).build().prompt().user(QUESTION).advisors(retrieval, new RagLoggingAdvisor()).call().content();
	}

	private static Document chunk(final String id, final String text, final int index, final int count) {
		final Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("source", "doc");
		metadata.put("ref", id.equals("laptops") ? "warranty" : id);
		metadata.put("section", id);
		metadata.put("chunking", "markdown");
		metadata.put("chunk_index", index);
		metadata.put("chunk_count", count);
		return Document.builder().id(id).text(text).metadata(metadata).build();
	}

	private static Map<String, Object> keyValues(final ILoggingEvent event) {
		final Map<String, Object> keyValues = new LinkedHashMap<>();
		final List<KeyValuePair> pairs = event.getKeyValuePairs() == null ? List.of() : event.getKeyValuePairs();
		pairs.forEach(pair -> keyValues.put(pair.key, pair.value));
		return keyValues;
	}

	/**
	 * Two dimensions: "laptop" or "warranty" in the text, and a constant. Enough to rank the warranty chunk first.
	 */
	private static final class ToyEmbeddingModel implements EmbeddingModel {

		@Override
		public EmbeddingResponse call(final EmbeddingRequest request) {
			final List<Embedding> embeddings = new ArrayList<>();
			for (int i = 0; i < request.getInstructions().size(); i++) {
				embeddings.add(new Embedding(vector(request.getInstructions().get(i)), i));
			}
			return new EmbeddingResponse(embeddings);
		}

		@Override
		public float[] embed(final Document document) {
			return vector(String.valueOf(document.getText()));
		}

		private static float[] vector(final String text) {
			final String lower = text.toLowerCase();
			return new float[]{lower.contains("laptop") || lower.contains("warranty") ? 1 : 0, 0.1f};
		}
	}
}