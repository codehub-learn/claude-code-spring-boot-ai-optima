package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U4.1 HTTP integration test for the catalog endpoints: the real context + {@code config.web.WebConfig} + Oracle + the {@code R__010}
 * Flyway baseline (6 categories, 24 products), driven through {@link MockMvcTester}. RestAssured against a real port and the OpenAPI spec
 * check are U4.5.
 */
@AutoConfigureMockMvc
class CatalogApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcClient jdbcClient;

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("delete from product where sku like 'API-%'").update();
	}

	@Test
	void listProductsIsPagedAndTheSizeIsClampedByConfig() {
		// application-test.yml sets app.catalog.max-page-size: 10, wired into the Pageable resolver by WebConfig.
		assertThat(mvc.get().uri("/api/v1/products").param("size", "50").param("sort", "sku"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.page.size").convertTo(Integer.class).isEqualTo(10);

		assertThat(mvc.get().uri("/api/v1/products").param("size", "50").param("sort", "sku"))
				.bodyJson().extractingPath("$.content").asArray().hasSize(10);
	}

	@Test
	void createThenGetRoundTrips() {
		final String body = """
							{"sku":"API-RT-1","name":"Round Trip SSD","description":"created by CatalogApiIT",
							 "price":123.45,"categoryName":"Storage"}""";

		assertThat(mvc.post().uri("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/products/API-RT-1");

		assertThat(mvc.get().uri("/api/v1/products/{sku}", "API-RT-1"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.categoryName").isEqualTo("Storage");
	}

	@Test
	void getUnknownProductReturns404() {
		assertThat(mvc.get().uri("/api/v1/products/{sku}", "API-UNKNOWN")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void listCategoriesReturnsTheBaseline() {
		assertThat(mvc.get().uri("/api/v1/categories"))
				.hasStatusOk()
				.bodyJson().extractingPath("$").asArray().hasSizeGreaterThanOrEqualTo(6);
	}
}