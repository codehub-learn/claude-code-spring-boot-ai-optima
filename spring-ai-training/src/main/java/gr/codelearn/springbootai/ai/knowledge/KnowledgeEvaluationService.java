package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.common.ChatModelUnavailableException;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse.QuestionResult;
import gr.codelearn.springbootai.ai.knowledge.dto.KnowledgeEvaluationResponse.StrategyResult;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.IntPredicate;

/**
 * Unit 10.2: measures retrieval quality per {@link ChunkingStrategy}, on the same store, without the chat model. The question set
 * ({@code classpath:evaluation/knowledge-questions.json}) gives each question the document that answers it and a phrase from the answer.
 *
 * <p>For every strategy, each question runs the same search the answers endpoint runs (top-k, filtered to that strategy's chunks plus the
 * catalog rows). A retrieved chunk is a <em>hit</em> when it comes from the expected document and contains the answer phrase, so a whole
 * document and a small chunk are judged alike: the answer has to be in what the model would get. Per strategy it reports:
 *
 * <ul>
 *   <li>hit@1 and hit@k: the share of questions whose first hit is at rank 1, or anywhere in the top k;</li>
 *   <li>MRR (mean reciprocal rank): 1 / rank of the first hit, averaged; 0 for a miss. Rewards finding the answer early;</li>
 *   <li>the mean and the lowest similarity of the first hit, and the highest similarity any chunk reached for the questions the store
 *   cannot answer: a similarity threshold only helps if it fits between the lowest hit and that highest miss;</li>
 *   <li>the mean context size: the tokens of the k retrieved chunks, what every question costs in prompt tokens.</li>
 * </ul>
 *
 * <p>Only embeddings are called (one per question and strategy), so it is cheap and repeatable after every ingestion.
 */
@Service
class KnowledgeEvaluationService {

	private static final JsonMapper JSON = JsonMapper.shared();

	private final VectorStore vectorStore;
	private final JdbcClient jdbcClient;
	private final List<EvaluationQuestion> questions;
	private final TokenCountEstimator tokenCounter = new JTokkitTokenCountEstimator();

	KnowledgeEvaluationService(final VectorStore vectorStore, final JdbcClient jdbcClient,
							   @Value("classpath:evaluation/knowledge-questions.json") final Resource questions) {
		this.vectorStore = vectorStore;
		this.jdbcClient = jdbcClient;
		this.questions = load(questions);
	}

	KnowledgeEvaluationResponse evaluate(final int topK) {
		final List<StrategyResult> strategies = new ArrayList<>();
		final Map<String, Map<String, @Nullable Integer>> ranksByQuestion = new LinkedHashMap<>();
		questions.forEach(question -> ranksByQuestion.put(question.question(), new LinkedHashMap<>()));
		for (final ChunkingStrategy strategy : ChunkingStrategy.values()) {
			final List<Integer> ranks = new ArrayList<>();
			final List<Double> hitSimilarities = new ArrayList<>();
			final List<Integer> contextTokens = new ArrayList<>();
			double maxUnanswerable = 0;
			for (final EvaluationQuestion question : questions) {
				final List<Document> retrieved = search(question.question(), strategy, topK);
				if (question.expectedRef() == null) {
					maxUnanswerable = Math.max(maxUnanswerable, retrieved.isEmpty() ? 0 : score(retrieved.getFirst()));
					continue;
				}
				final int rank = firstHit(retrieved, question);
				ranks.add(rank);
				ranksByQuestion.get(question.question()).put(strategy.key(), rank == 0 ? null : rank);
				if (rank > 0) {
					hitSimilarities.add(score(retrieved.get(rank - 1)));
				}
				contextTokens.add(retrieved.stream().mapToInt(document -> tokenCounter.estimate(document.getText())).sum());
			}
			strategies.add(new StrategyResult(strategy.key(), rows(strategy), share(ranks, rank -> rank == 1),
											  share(ranks, rank -> rank > 0),
			                                  round(ranks.stream().mapToDouble(rank -> rank == 0 ? 0 : 1d / rank).average()),
											  round(hitSimilarities.stream().mapToDouble(Double::doubleValue).average()),
											  round(hitSimilarities.stream().mapToDouble(Double::doubleValue).min()),
			                                  round(maxUnanswerable),
											  (int) Math.round(contextTokens.stream().mapToInt(Integer::intValue).average().orElse(0))));
		}
		final List<QuestionResult> details = questions.stream()
													  .map(question -> new QuestionResult(question.question(), question.expectedRef(),
																						  ranksByQuestion.get(question.question())))
													  .toList();
		return new KnowledgeEvaluationResponse(topK, questions.size(), strategies, details);
	}

	private List<Document> search(final String query, final ChunkingStrategy strategy, final int topK) {
		try {
			return vectorStore.similaritySearch(SearchRequest.builder()
															 .query(query)
															 .topK(topK)
															 .filterExpression(strategy.filterExpression())
															 .build());
		} catch (RuntimeException ex) {
			throw new ChatModelUnavailableException("Evaluation failed: the embedding model or the vector store is unavailable", ex);
		}
	}

	/**
	 * The 1-based rank of the first chunk from the expected document that contains the answer phrase, or 0 when none of the top k does.
	 * Whitespace is normalised on both sides: a splitter may have turned a line break into a space.
	 */
	static int firstHit(final List<Document> retrieved, final EvaluationQuestion question) {
		final String answer = normalise(String.valueOf(question.answer()));
		for (int index = 0; index < retrieved.size(); index++) {
			final Document document = retrieved.get(index);
			final boolean fromExpected = String.valueOf(question.expectedRef())
											   .equals(KnowledgeService.text(document.getMetadata().get("ref")));
			if (fromExpected && normalise(String.valueOf(document.getText())).contains(answer)) {
				return index + 1;
			}
		}
		return 0;
	}

	private int rows(final ChunkingStrategy strategy) {
		return jdbcClient.sql("select count(*) from knowledge_vector where json_value(metadata, '$.chunking') = ?")
						 .param(strategy.key())
						 .query(Integer.class)
						 .single();
	}

	private static double score(final Document document) {
		return document.getScore() == null ? 0 : document.getScore();
	}

	private static double share(final List<Integer> ranks, final IntPredicate hit) {
		return ranks.isEmpty() ? 0 : round((double) ranks.stream().filter(hit::test).count() / ranks.size());
	}

	private static double round(final OptionalDouble value) {
		return round(value.orElse(0));
	}

	private static double round(final double value) {
		return Math.round(value * 1000) / 1000d;
	}

	private static String normalise(final String text) {
		return text.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static List<EvaluationQuestion> load(final Resource resource) {
		try (InputStream input = resource.getInputStream()) {
			return JSON.readValue(input, new TypeReference<List<EvaluationQuestion>>() {
			});
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot read the evaluation questions", ex);
		}
	}

	/**
	 * One evaluation question.
	 *
	 * @param question    what a customer might ask
	 * @param expectedRef the document (file name) or SKU that answers it; {@code null} for a question the store cannot answer
	 * @param answer      a phrase the answering chunk must contain; {@code null} with {@code expectedRef}
	 */
	record EvaluationQuestion(String question, @Nullable String expectedRef, @Nullable String answer) {
	}
}