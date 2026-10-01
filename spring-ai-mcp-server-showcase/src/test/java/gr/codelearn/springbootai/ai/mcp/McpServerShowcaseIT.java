package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.support.AbstractCatalogIT;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog tools over a real port and a real Oracle, in the default {@code mcp} style (unit 12.2). {@link ToolStyleIT} runs every test
 * here again against the {@code tool} style (unit 12.1), which is the proof that a client cannot tell the two annotation styles apart by
 * behaviour; the hints test is the one place they differ and is overridden there.
 */
class McpServerShowcaseIT extends AbstractCatalogIT {

	protected McpSyncClient client;

	@BeforeEach
	void connectClient() {
		client = connect();
	}

	@Test
	void listsTheCatalogToolsAndTheEnrichmentToolWithDescriptionsAndSchemas() {
		final var tools = client.listTools().tools();

		assertThat(tools).extracting(Tool::name)
						 .containsExactlyInAnyOrder("searchProducts", "getProduct", "listCategories", "enrichProduct");
		assertThat(tools).allSatisfy(tool -> {
			assertThat(tool.description()).isNotBlank();
			assertThat(tool.inputSchema()).isNotNull();
		});
	}

	@Test
	void marksOnlyTheSkuAsRequired() {
		final var tools = client.listTools().tools();

		assertThat(requiredArguments(tools, "searchProducts")).isEmpty();
		assertThat(requiredArguments(tools, "getProduct")).containsExactly("sku");
		assertThat(requiredArguments(tools, "listCategories")).isEmpty();
	}

	@Test
	void mcpToolsCarryReadOnlyHints() {
		final var tool = client.listTools().tools().stream().filter(t -> t.name().equals("searchProducts")).findFirst().orElseThrow();

		assertThat(tool.annotations()).isNotNull();
		assertThat(tool.annotations().title()).isEqualTo("Search products");
		assertThat(tool.annotations().readOnlyHint()).isTrue();
		assertThat(tool.annotations().destructiveHint()).isFalse();
		assertThat(tool.annotations().idempotentHint()).isTrue();
		assertThat(tool.annotations().openWorldHint()).isFalse();
	}

	@Test
	void listsCategoriesWithCountsAndNoDemoRows() {
		final String text = call(client, "listCategories", Map.of());

		assertThat(text).contains("Laptops").contains("\"productCount\":2");
		assertThat(text).contains("Audio").contains("\"productCount\":1");
		assertThat(text).contains("Empty").contains("\"productCount\":0");
	}

	@Test
	void searchesByTextCategoryAndPriceAndSkipsDemoRows() {
		assertThat(call(client, "searchProducts", Map.of("query", "laptop"))).contains("LAP-001")
																			 .contains("LAP-002")
																			 .doesNotContain("AUD-001");
		assertThat(call(client, "searchProducts", Map.of("category", "audio"))).contains("AUD-001").doesNotContain("GEN-0001");
		assertThat(call(client, "searchProducts", Map.of("maxPrice", 1500))).contains("LAP-001")
																			.contains("AUD-001")
																			.doesNotContain("LAP-002");
		assertThat(call(client, "searchProducts", Map.of("limit", 1))).contains("AUD-001").doesNotContain("LAP-001");
	}

	@Test
	void treatsWildcardsInTheQueryAsLiteralText() {
		assertThat(call(client, "searchProducts", Map.of("query", "100%"))).contains("LAP-001").doesNotContain("LAP-002");
		assertThat(call(client, "searchProducts", Map.of("query", "20_000"))).contains("AUD-001");
		assertThat(call(client, "searchProducts", Map.of("query", "20x000"))).isEqualTo("[]");
	}

	@Test
	void getsOneProductBySkuIgnoringCase() {
		assertThat(call(client, "getProduct", Map.of("sku", "lap-002"))).contains("WorkStation 16")
																		.contains("2199")
																		.contains("Powerful 16 inch");
	}

	@Test
	void reportsAnUnknownSkuAsAnErrorResultNotAnException() {
		final CallToolResult result = client.callTool(new CallToolRequest("getProduct", Map.of("sku", "NOPE-1")));

		assertThat(result.isError()).isTrue();
		assertThat(result.content()).singleElement().isInstanceOfSatisfying(TextContent.class,
																			text -> assertThat(text.text()).contains("NOPE-1"));
	}

	@Test
	void reportsAGeneratedDemoSkuAsUnknown() {
		assertThat(client.callTool(new CallToolRequest("getProduct", Map.of("sku", "GEN-0001"))).isError()).isTrue();
	}

	@Test
	void rejectsANegativeMaxPrice() {
		assertThat(client.callTool(new CallToolRequest("searchProducts", Map.of("maxPrice", -1))).isError()).isTrue();
	}

	@SuppressWarnings("unchecked")
	protected static List<String> requiredArguments(final List<Tool> tools, final String name) {
		final Object required = tools.stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow().inputSchema().get("required");
		return required == null ? List.of() : (List<String>) required;
	}
}