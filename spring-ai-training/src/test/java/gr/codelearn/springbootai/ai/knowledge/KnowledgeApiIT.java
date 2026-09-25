package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResultAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.1 integration test: the whole RAG flow over HTTP, on the Testcontainers Oracle and its real vector table, with two stubs. The
 * {@link HashingEmbeddingModel} maps each word to one of 1024 dimensions (texts that share words are close, no model needed); the
 * {@link RecordingChatModel} records the prompt and answers "Answer". Catalog rows come from the {@code knowledge-catalog.sql} fixture.
 */
@AutoConfigureMockMvc
@Sql("/sql/knowledge-catalog.sql")
class KnowledgeApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RecordingChatModel chatModel;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void reset() {
		chatModel.reset();
	}

	@Test
	void ingestionEmbedsTheCatalogWithoutDemoRowsAndTheBundledDocuments() {
		ingest().hasStatusOk().bodyJson().isLenientlyEqualTo("""
															 {"catalogDocuments": 3, "bundledDocuments": 4, "totalDocuments": 7}""");

		assertThat(storedRefs()).containsExactlyInAnyOrder("LAP-0002", "LAP-0003", "AUD-0001", "laptop-buying-guide", "returns-policy",
														   "shipping", "warranty");
	}

	@Test
	void ingestingTwiceReplacesInsteadOfDuplicating() {
		ingest().hasStatusOk();
		ingest().hasStatusOk();

		assertThat(storedRefs()).hasSize(7);
	}

	@Test
	void anAnswerIsGeneratedFromTheRetrievedDocuments() {
		ingest().hasStatusOk();

		ask("Which workstation has a discrete GPU and 32 GB RAM?").hasStatusOk()
																  .bodyJson()
																  .extractingPath("$.sources[0].ref")
																  .isEqualTo("LAP-0002");

		// Query -> retrieval -> context -> generation: the model got the system prompt, then one user message built from the template
		// with the retrieved document's text (tag included) inside <documents> and the question last.
		final List<Message> messages = chatModel.prompts().getLast().getInstructions();
		assertThat(messages).extracting(Message::getMessageType).containsExactly(MessageType.SYSTEM, MessageType.USER);
		// ST writes line breaks as the platform's (\r\n on Windows), so compare with \n.
		assertThat(messages.getLast().getText().replace("\r\n", "\n")).contains("<documents>",
		                                                                        "[catalog:LAP-0002] Latitude 16 Workstation. Category: " +
																				"Laptops.")
																	  .endsWith(
				                                                              "Which workstation has a discrete GPU and 32 GB " +
																			  "RAM?\n</question>");
	}

	@Test
	void topKBoundsTheContext() {
		ingest().hasStatusOk();

		ask("What is the warranty on laptops?").hasStatusOk().bodyJson().extractingPath("$.sources").asArray().hasSize(4);
	}

	@Test
	void anEmptyStoreStillAnswersWithNoSources() {
		jdbcClient.sql("delete from knowledge_vector").update();

		ask("Which workstation has a discrete GPU?").hasStatusOk().bodyJson().extractingPath("$.sources").asArray().isEmpty();
		assertThat(chatModel.prompts()).hasSize(1);
	}

	private MvcTestResultAssert ingest() {
		return assertThat(mvc.post().uri("/api/v1/knowledge/ingestions"));
	}

	private MvcTestResultAssert ask(final String question) {
		return assertThat(mvc.post().uri("/api/v1/knowledge/answers").contentType(MediaType.APPLICATION_JSON).content("""
																													  {"question": "%s"}"""
																															  .formatted(
						                                                                                                              question)));
	}

	private List<String> storedRefs() {
		return jdbcClient.sql("select json_value(metadata, '$.ref') from knowledge_vector").query(String.class).list();
	}

	@TestConfiguration
	static class StubModelConfig {

		@Bean
		@Primary
		RecordingChatModel recordingChatModel() {
			return new RecordingChatModel();
		}

		@Bean
		@Primary
		HashingEmbeddingModel hashingEmbeddingModel() {
			return new HashingEmbeddingModel();
		}
	}

	static class RecordingChatModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public synchronized ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Answer"))),
									ChatResponseMetadata.builder().model("stub-model").usage(new DefaultUsage(400, 1)).build());
		}

		synchronized List<Prompt> prompts() {
			return List.copyOf(prompts);
		}

		synchronized void reset() {
			prompts.clear();
		}
	}

	/**
	 * A deterministic stand-in for an embedding model: every lower-cased word adds 1 to the dimension its hash picks. Texts that share
	 * words point in similar directions, which is all the tests need; the real model also captures meaning ("3D rendering" near "GPU").
	 */
	static class HashingEmbeddingModel implements EmbeddingModel {

		static final int DIMENSIONS = 1024;

		@Override
		public EmbeddingResponse call(final EmbeddingRequest request) {
			final List<String> texts = request.getInstructions();
			return new EmbeddingResponse(IntStream.range(0, texts.size()).mapToObj(i -> new Embedding(vector(texts.get(i)), i)).toList());
		}

		@Override
		public float[] embed(final Document document) {
			return vector(document.getText() == null ? "" : document.getText());
		}

		@Override
		public int dimensions() {
			return DIMENSIONS;
		}

		private static float[] vector(final String text) {
			final float[] vector = new float[DIMENSIONS];
			for (final String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
				if (word.length() > 2) {
					vector[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1;
				}
			}
			vector[0] += 0.01f; // never all zeros: a zero vector has no direction, and cosine distance is undefined for it
			return vector;
		}
	}
}