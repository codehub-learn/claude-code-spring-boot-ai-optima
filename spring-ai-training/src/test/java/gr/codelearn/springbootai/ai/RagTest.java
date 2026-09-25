package gr.codelearn.springbootai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.1: what Spring AI 2.0.1's {@link QuestionAnswerAdvisor} does, proven with an in-memory {@link SimpleVectorStore}, a toy embedding
 * model and a stub chat model (no database, no Spring context). The application runs the same advisor over Oracle.
 */
class RagTest {

	private final RecordingModel chatModel = new RecordingModel();
	private final SimpleVectorStore vectorStore = SimpleVectorStore.builder(new ToyEmbeddingModel()).build();

	@Test
	void theModelSeesTheRetrievedTextButNeverTheMetadata() {
		vectorStore.add(List.of(new Document("Laptops carry a 2-year warranty.", Map.of("ref", "warranty-doc-7"))));

		ask(QuestionAnswerAdvisor.builder(vectorStore).build(), "How long is the laptop warranty?");

		// So anything the model must cite (a SKU, a document name) has to be written into the text at ingestion.
		assertThat(chatModel.lastUserText()).contains("Laptops carry a 2-year warranty.").doesNotContain("warranty-doc-7");
	}

	@Test
	void theRetrievedDocumentsComeBackInTheResponseMetadata() {
		vectorStore.add(List.of(new Document("Laptops carry a 2-year warranty.")));

		final ChatResponse response = ask(QuestionAnswerAdvisor.builder(vectorStore).build(), "Laptop warranty?");

		assertThat((List<?>) response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS)).hasSize(1);
	}

	@Test
	void topKLimitsTheContext() {
		vectorStore.add(List.of(new Document("Laptop A"), new Document("Laptop B"), new Document("Laptop C")));

		final ChatResponse response = ask(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(SearchRequest.builder().topK(2).build())
															   .build(), "Laptop?");

		assertThat((List<?>) response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS)).hasSize(2);
	}

	@Test
	void pitfallTheDefaultOrderTiesWithTheLogger() {
		// Two advisors with the same order run in no guaranteed order: the logger may print the prompt before the context is added.
		// config.ai.RagConfig sets an explicit, earlier order.
		assertThat(QuestionAnswerAdvisor.builder(vectorStore).build().getOrder()).isEqualTo(new SimpleLoggerAdvisor().getOrder());
	}

	private ChatResponse ask(final QuestionAnswerAdvisor advisor, final String question) {
		return ChatClient.builder(chatModel).build().prompt().user(question).advisors(advisor).call().chatResponse();
	}

	private static final class RecordingModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();

		@Override
		public ChatResponse call(final Prompt prompt) {
			prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Answer"))));
		}

		String lastUserText() {
			return prompts.getLast().getUserMessage().getText();
		}
	}

	/**
	 * Three dimensions: how often "laptop", "warranty" and anything else appear. Enough for "which document shares my words".
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
			return new float[]{lower.contains("laptop") ? 1 : 0, lower.contains("warranty") ? 1 : 0, 0.1f};
		}
	}
}