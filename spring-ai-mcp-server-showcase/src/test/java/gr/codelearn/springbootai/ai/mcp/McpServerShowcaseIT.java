package gr.codelearn.springbootai.ai.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The server over a real port and a real Oracle, driven by the MCP Java SDK's own client: the same handshake (initialize, list, call)
 * Claude Code does. The catalog tables are created in the container user's schema by the scripts in {@code src/test/resources/sql}, so the
 * test needs no spring-boot-training database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:sql/catalog-schema.sql",
		"spring.sql.init.data-locations=classpath:sql/catalog-data.sql",
		"app.catalog.schema=test"
})
@ActiveProfiles("test")
class McpServerShowcaseIT {

	@ServiceConnection
	static final OracleContainer ORACLE = new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"));

	static {
		ORACLE.start();
	}

	@LocalServerPort
	private int port;

	private McpSyncClient client;

	@BeforeEach
	void connect() {
		final var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build();
		client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(20)).build();
		client.initialize();
	}

	@AfterEach
	void disconnect() {
		client.close();
	}

	@Test
	void listsTheThreeCatalogToolsWithDescriptionsAndSchemas() {
		final var tools = client.listTools().tools();

		assertThat(tools).extracting(Tool::name).containsExactlyInAnyOrder("searchProducts", "getProduct", "listCategories");
		assertThat(tools).allSatisfy(tool -> {
			assertThat(tool.description()).isNotBlank();
			assertThat(tool.inputSchema()).isNotNull();
		});
	}

	@Test
	void listsCategoriesWithCountsAndNoDemoRows() {
		final String text = call("listCategories", Map.of());

		assertThat(text).contains("Laptops").contains("\"productCount\":2");
		assertThat(text).contains("Audio").contains("\"productCount\":1");
		assertThat(text).contains("Empty").contains("\"productCount\":0");
	}

	@Test
	void searchesByTextCategoryAndPriceAndSkipsDemoRows() {
		assertThat(call("searchProducts", Map.of("query", "laptop"))).contains("LAP-001").contains("LAP-002").doesNotContain("AUD-001");
		assertThat(call("searchProducts", Map.of("category", "audio"))).contains("AUD-001").doesNotContain("GEN-0001");
		assertThat(call("searchProducts", Map.of("maxPrice", 1500))).contains("LAP-001").contains("AUD-001").doesNotContain("LAP-002");
		assertThat(call("searchProducts", Map.of("limit", 1))).contains("AUD-001").doesNotContain("LAP-001");
	}

	@Test
	void treatsWildcardsInTheQueryAsLiteralText() {
		assertThat(call("searchProducts", Map.of("query", "100%"))).contains("LAP-001").doesNotContain("LAP-002");
		assertThat(call("searchProducts", Map.of("query", "20_000"))).contains("AUD-001");
		assertThat(call("searchProducts", Map.of("query", "20x000"))).isEqualTo("[]");
	}

	@Test
	void getsOneProductBySkuIgnoringCase() {
		assertThat(call("getProduct", Map.of("sku", "lap-002"))).contains("WorkStation 16").contains("2199").contains("Powerful 16 inch");
	}

	@Test
	void reportsAnUnknownSkuAsAnErrorResultNotAnException() {
		final CallToolResult result = client.callTool(new CallToolRequest("getProduct", Map.of("sku", "NOPE-1")));

		assertThat(result.isError()).isTrue();
		assertThat(result.content()).singleElement().isInstanceOfSatisfying(TextContent.class,
																			text -> assertThat(text.text()).contains("NOPE-1"));
	}

	@Test
	void reportsAnGeneratedDemoSkuAsUnknown() {
		assertThat(client.callTool(new CallToolRequest("getProduct", Map.of("sku", "GEN-0001"))).isError()).isTrue();
	}

	@Test
	void rejectsANegativeMaxPrice() {
		assertThat(client.callTool(new CallToolRequest("searchProducts", Map.of("maxPrice", -1))).isError()).isTrue();
	}

	private String call(final String tool, final Map<String, Object> arguments) {
		final CallToolResult result = client.callTool(new CallToolRequest(tool, arguments));
		assertThat(result.isError()).as("%s: %s", tool, result.content()).isFalse();
		return ((TextContent) result.content().getFirst()).text();
	}
}