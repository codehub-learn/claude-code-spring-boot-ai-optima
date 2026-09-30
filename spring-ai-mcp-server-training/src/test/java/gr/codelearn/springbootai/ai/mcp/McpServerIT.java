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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The server over a real port, driven by the MCP Java SDK's own client: the same handshake (initialize, list, call) any MCP client does,
 * spring-ai-training's included.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class McpServerIT {

	@LocalServerPort
	private int port;

	private McpSyncClient client;

	@BeforeEach
	void connect() {
		final var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build();
		client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
		client.initialize();
	}

	@AfterEach
	void disconnect() {
		client.close();
	}

	@Test
	void listsBothToolsWithDescriptionsAndSchemas() {
		final var tools = client.listTools().tools();

		assertThat(tools).extracting(Tool::name).containsExactlyInAnyOrder("currentTime", "add");
		assertThat(tools).allSatisfy(tool -> {
			assertThat(tool.description()).isNotBlank();
			assertThat(tool.inputSchema()).isNotNull();
		});
	}

	@Test
	void callsAToolAndReturnsText() {
		final CallToolResult result = client.callTool(new CallToolRequest("add", Map.of("a", 19.99, "b", 0.01)));

		assertThat(result.isError()).isFalse();
		assertThat(result.content()).singleElement().isInstanceOfSatisfying(TextContent.class,
																			text -> assertThat(text.text()).contains("20.00"));
	}

	@Test
	void reportsAToolFailureAsAnErrorResultNotAnException() {
		final CallToolResult result = client.callTool(new CallToolRequest("currentTime", Map.of("zoneId", "Mars/Olympus")));

		assertThat(result.isError()).isTrue();
		assertThat(result.content()).singleElement().isInstanceOfSatisfying(TextContent.class,
																			text -> assertThat(text.text()).contains("Mars/Olympus"));
	}
}