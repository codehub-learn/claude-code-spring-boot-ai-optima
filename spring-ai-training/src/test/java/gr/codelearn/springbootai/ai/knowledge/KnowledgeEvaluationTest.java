package gr.codelearn.springbootai.ai.knowledge;

import gr.codelearn.springbootai.ai.knowledge.KnowledgeEvaluationService.EvaluationQuestion;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 10.2: what counts as a hit in the retrieval evaluation. No store, no model.
 */
class KnowledgeEvaluationTest {

	private static final EvaluationQuestion CASH_ON_DELIVERY = new EvaluationQuestion("Cash on delivery fee?", "terms-of-sale",
	                                                                                  "a fee of 2 EUR");

	@Test
	void aHitComesFromTheExpectedDocumentAndContainsTheAnswer() {
		final List<Document> retrieved = List.of(chunk("terms-of-sale", "Card payments are authorised when you order."),
												 chunk("terms-of-sale", "Cash on delivery is available, with a fee of 2 EUR."));

		assertThat(KnowledgeEvaluationService.firstHit(retrieved, CASH_ON_DELIVERY)).isEqualTo(2);
	}

	@Test
	void theAnswerInAnotherDocumentIsNotAHit() {
		final List<Document> retrieved = List.of(chunk("shipping", "Cash on delivery costs a fee of 2 EUR."));

		assertThat(KnowledgeEvaluationService.firstHit(retrieved, CASH_ON_DELIVERY)).isZero();
	}

	@Test
	void whitespaceAndCaseDoNotMatter() {
		// A splitter may turn the line break inside "a fee of\n2 EUR" into a space, or keep it.
		final List<Document> retrieved = List.of(chunk("terms-of-sale", "...up to 500 EUR, with A FEE OF\r\n2 EUR."));

		assertThat(KnowledgeEvaluationService.firstHit(retrieved, CASH_ON_DELIVERY)).isEqualTo(1);
	}

	@Test
	void theOracleJsonQuotesOnTheRefAreIgnored() {
		// Metadata read back from Oracle's JSON column keeps the JSON quotes (10.1): "\"terms-of-sale\"".
		final List<Document> retrieved = List.of(new Document("with a fee of 2 EUR", Map.of("ref", new Object() {
			@Override
			public String toString() {
				return "\"terms-of-sale\"";
			}
		})));

		assertThat(KnowledgeEvaluationService.firstHit(retrieved, CASH_ON_DELIVERY)).isEqualTo(1);
	}

	private static Document chunk(final String ref, final String text) {
		return new Document(text, Map.of("ref", ref));
	}
}