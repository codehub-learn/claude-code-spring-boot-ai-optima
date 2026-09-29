package gr.codelearn.springbootai.ai.product;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.1: how a search query is cut into keywords. The SQL itself runs against Oracle in {@code ProductApiIT}.
 */
class ProductServiceTest {

	@Test
	void punctuationAndCommasSeparateWordsAndCaseIsIgnored() {
		// What a model wrote in a live run, before maxPrice existed.
		assertThat(ProductService.keywords("Laptop, under 1500 euro; price!")).containsExactly("laptop", "under", "1500", "euro", "price");
	}

	@Test
	void likeWildcardsNeverReachTheQuery() {
		assertThat(ProductService.keywords("100% _noise_")).containsExactly("100", "noise");
	}

	@Test
	void dashesAndOtherScriptsStayInAWord() {
		assertThat(ProductService.keywords("LAP-0002 ακουστικά")).containsExactly("lap-0002", "ακουστικά");
	}

	@Test
	void onlyTheFirstFiveWordsCount() {
		assertThat(ProductService.keywords("a b c d e f g")).hasSize(ProductService.MAX_KEYWORDS).endsWith("e");
	}

	@Test
	void aBlankQueryHasNoKeywords() {
		assertThat(ProductService.keywords("  ")).isEmpty();
		assertThat(ProductService.keywords(null)).isEmpty();
	}
}