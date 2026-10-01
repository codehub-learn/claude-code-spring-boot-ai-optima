package gr.codelearn.springbootai.ai.mcp.support;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.oracle.OracleContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base of the integration tests: the server over a real port and a real Oracle, driven by the MCP Java SDK's own client (the same handshake
 * Claude Code does). The catalog tables are created in the container user's schema by the scripts in {@code src/test/resources/sql}, so the
 * tests need neither compose nor spring-boot-training. A subclass that needs another configuration adds a {@code @TestPropertySource},
 * which merges with the properties here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:sql/catalog-schema.sql",
		"spring.sql.init.data-locations=classpath:sql/catalog-data.sql",
		"app.catalog.schema=test"
})
@ActiveProfiles("test")
public abstract class AbstractCatalogIT {

	@ServiceConnection
	protected static final OracleContainer ORACLE = SharedOracleContainer.INSTANCE;

	@LocalServerPort
	private int port;

	private final List<McpSyncClient> clients = new ArrayList<>();

	/**
	 * An initialized client; the customizer adds capabilities and handlers (sampling, progress, logging) before the handshake.
	 */
	protected McpSyncClient connect(final UnaryOperator<McpClient.SyncSpec> customizer) {
		final var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build();
		final McpSyncClient client = customizer.apply(McpClient.sync(transport).requestTimeout(Duration.ofSeconds(20))).build();
		client.initialize();
		clients.add(client);
		return client;
	}

	protected McpSyncClient connect() {
		return connect(UnaryOperator.identity());
	}

	@AfterEach
	void disconnectClients() {
		clients.forEach(McpSyncClient::close);
		clients.clear();
	}

	/**
	 * Calls a tool, asserts it did not fail, and returns its text.
	 */
	protected static String call(final McpSyncClient client, final String tool, final Map<String, Object> arguments) {
		final CallToolResult result = client.callTool(new CallToolRequest(tool, arguments));
		assertThat(result.isError()).as("%s: %s", tool, result.content()).isFalse();
		return ((TextContent) result.content().getFirst()).text();
	}
}