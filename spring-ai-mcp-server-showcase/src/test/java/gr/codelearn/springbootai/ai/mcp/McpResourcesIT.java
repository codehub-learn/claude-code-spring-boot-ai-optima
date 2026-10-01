package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.support.AbstractCatalogIT;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The catalog as MCP resources (unit 12.2): listed, templated and read through the protocol, the way a client's {@code @}-mention does.
 */
class McpResourcesIT extends AbstractCatalogIT {

	private McpSyncClient client;

	@BeforeEach
	void connectClient() {
		client = connect();
	}

	@Test
	void listsTheFixedResourceAndKeepsTheTemplateApart() {
		assertThat(client.listResources().resources()).extracting(Resource::uri).containsExactly("catalog://categories");
		assertThat(client.listResourceTemplates().resourceTemplates()).extracting(ResourceTemplate::uriTemplate)
																	  .containsExactly("catalog://product/{sku}");
	}

	@Test
	void describesTheResourcesForTheClient() {
		final Resource categories = client.listResources().resources().getFirst();

		assertThat(categories.name()).isEqualTo("categories");
		assertThat(categories.mimeType()).isEqualTo("application/json");
		assertThat(categories.description()).isNotBlank();
	}

	@Test
	void readsTheCategoriesAsJson() {
		final String text = read("catalog://categories");

		assertThat(text).contains("\"name\":\"Laptops\"").contains("\"productCount\":2").contains("\"name\":\"Empty\"");
	}

	@Test
	void readsOneProductThroughTheTemplateIgnoringCase() {
		final String text = read("catalog://product/lap-002");

		assertThat(text).contains("\"sku\":\"LAP-002\"").contains("WorkStation 16").contains("Powerful 16 inch");
	}

	@Test
	void failsToReadAnUnknownProduct() {
		assertThatThrownBy(() -> read("catalog://product/NOPE-1")).hasMessageContaining("NOPE-1");
	}

	@Test
	void failsToReadAGeneratedDemoProduct() {
		assertThatThrownBy(() -> read("catalog://product/GEN-0001")).isInstanceOf(RuntimeException.class);
	}

	private String read(final String uri) {
		final ReadResourceResult result = client.readResource(new ReadResourceRequest(uri));
		assertThat(result.contents()).singleElement().isInstanceOf(TextResourceContents.class);
		return ((TextResourceContents) result.contents().getFirst()).text();
	}
}