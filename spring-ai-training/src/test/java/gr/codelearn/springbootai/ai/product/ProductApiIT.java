package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.1 integration test: the product REST API against Oracle, over the {@code knowledge-catalog.sql} fixture (LAP-0002 Latitude 16,
 * LAP-0003 Flex 13, AUD-0001 Studio 3, and a GEN-* demo row).
 */
@AutoConfigureMockMvc
@Sql("/sql/knowledge-catalog.sql")
class ProductApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void anyKeywordMatchesAndTheBestMatchComesFirst() {
		// "convertible" and "pen" both match the Flex 13; "laptop" matches both laptops by category.
		assertThat(mvc.get().uri("/api/v1/products?query=laptop convertible pen")).hasStatusOk()
																				  .bodyJson()
																				  .extractingPath("$[*].sku")
																				  .asArray().containsExactly("LAP-0003", "LAP-0002");
	}

	@Test
	void maxPriceCapsThePriceAndDemoRowsAreLeftOut() {
		assertThat(mvc.get().uri("/api/v1/products?max-price=1500")).hasStatusOk()
																	.bodyJson()
																	.extractingPath("$[*].sku")
																	.asArray().containsExactly("LAP-0003", "AUD-0001");
	}

	@Test
	void getIsCaseInsensitiveAndPricesKeepTwoDecimals() {
		assertThat(mvc.get().uri("/api/v1/products/lap-0002")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
																										   {"sku": "LAP-0002", "category": "Laptops", "price": 2199.00, "currency": "EUR"}""");
		assertThat(mvc.get().uri("/api/v1/products/lap-0002")).bodyText().contains("\"price\":2199.00");
	}

	@Test
	void anUnknownSkuIsA404() {
		assertThat(mvc.get().uri("/api/v1/products/AUD-9999")).hasStatus(HttpStatus.NOT_FOUND);
	}
}