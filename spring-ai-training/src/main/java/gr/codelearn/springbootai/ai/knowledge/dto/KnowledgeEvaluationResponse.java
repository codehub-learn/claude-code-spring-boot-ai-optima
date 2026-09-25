package gr.codelearn.springbootai.ai.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Unit 10.2: retrieval quality per chunking strategy, measured on the bundled question set.
 *
 * @param topK       how many chunks each search returned
 * @param questions  how many questions were asked (answerable and not)
 * @param strategies one result per chunking strategy
 * @param details    per question, the rank of the first hit under each strategy
 */
public record KnowledgeEvaluationResponse(
		@Schema(example = "4") int topK,
		@Schema(example = "19") int questions,
		@Schema(description = "One result per chunking strategy") List<StrategyResult> strategies,
		@Schema(description = "Per question, the first hit's rank under each strategy") List<QuestionResult> details) {

	/**
	 * How well one chunking strategy retrieved.
	 *
	 * @param strategy                  {@code whole}, {@code token} or {@code markdown}
	 * @param chunks                    how many chunks the strategy stored (catalog rows not counted)
	 * @param hitAt1                    share of answerable questions whose answer was in the first chunk
	 * @param hitAtK                    share of answerable questions whose answer was anywhere in the top k
	 * @param mrr                       mean reciprocal rank of the first hit (0 for a miss)
	 * @param meanHitSimilarity         the average similarity of the first hit
	 * @param minHitSimilarity          the lowest similarity of a first hit: a similarity threshold above it loses that answer
	 * @param maxUnanswerableSimilarity the highest similarity any chunk reached for a question the store cannot answer
	 * @param meanContextTokens         the average tokens of the k retrieved chunks: the context each question costs
	 */
	public record StrategyResult(
			@Schema(example = "markdown") String strategy,
			@Schema(example = "61") int chunks,
			@Schema(example = "0.824") double hitAt1,
			@Schema(example = "0.941") double hitAtK,
			@Schema(example = "0.87") double mrr,
			@Schema(example = "0.62") double meanHitSimilarity,
			@Schema(example = "0.45") double minHitSimilarity,
			@Schema(example = "0.41") double maxUnanswerableSimilarity,
			@Schema(example = "310") int meanContextTokens) {
	}

	/**
	 * One question's outcome.
	 *
	 * @param question    the question
	 * @param expectedRef the document or SKU that answers it ({@code null}: not answerable)
	 * @param ranks       per strategy, the 1-based rank of the first hit, {@code null} for a miss (empty for an unanswerable question)
	 */
	public record QuestionResult(
			@Schema(example = "Is there an extra fee for paying cash on delivery?") String question,
			@Schema(example = "terms-of-sale") @Nullable String expectedRef,
			@Schema(example = "{\"whole\": 2, \"token\": 1, \"markdown\": 1}") Map<String, @Nullable Integer> ranks) {
	}
}