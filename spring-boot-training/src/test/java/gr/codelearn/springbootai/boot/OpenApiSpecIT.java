package gr.codelearn.springbootai.boot;

import gr.codelearn.springbootai.boot.support.AbstractOracleIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U4.5: a sanity check that springdoc actually publishes the document {@code config.web.OpenApiConfig} describes — not a full contract test
 * (that would restate every {@code @Operation}/{@code @ApiResponse} annotation as an assertion and break on every harmless controller
 * change), just proof the spec exists, carries the right document metadata, and lists the resources this unit built.
 *
 * <p>{@code @AutoConfigureMockMvc} on the default ({@code MOCK}) web environment reuses the same cached context as
 * {@code CatalogApiIT}/{@code OrderApiIT} - no extra Oracle container or context startup cost, unlike {@link RestAssuredApiIT}'s
 * {@code RANDOM_PORT}.
 */
@AutoConfigureMockMvc
class OpenApiSpecIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void apiDocsCarriesTheDocumentMetadata() {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.info.title").isEqualTo("Code.Learn Spring Boot Training API");

		assertThat(mvc.get().uri("/v3/api-docs"))
				.bodyJson().extractingPath("$.tags[*].name").asArray().contains("catalog", "orders");
	}

	@Test
	void apiDocsListsEveryResourceBuiltInThisUnit() {
		// Dot notation breaks on the "/" inside each path key; JsonPath needs the bracketed form.
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.hasPath("$.paths['/api/v1/products']")
				.hasPath("$.paths['/api/v1/products/{sku}']")
				.hasPath("$.paths['/api/v1/categories']")
				.hasPath("$.paths['/api/v1/categories/{name}']")
				.hasPath("$.paths['/api/v1/orders']")
				.hasPath("$.paths['/api/v1/orders/{orderNumber}']")
				.hasPath("$.paths['/api/v1/orders/{orderNumber}/status']");
	}

	@Test
	void swaggerUiIsServed() {
		// springdoc redirects /swagger-ui.html to the actual UI resource (/swagger-ui/index.html) rather than serving it directly.
		assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus3xxRedirection();
	}
}