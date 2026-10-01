package gr.codelearn.springbootai.ai.mcp;

import gr.codelearn.springbootai.ai.mcp.support.AbstractCatalogIT;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The tool only the MCP annotations can express (unit 12.2): while it runs, the server asks the client's model for text (sampling), reports
 * progress and sends log messages. The client here plays the model with a fixed answer and records everything it is sent.
 */
class EnrichProductIT extends AbstractCatalogIT {

	private final List<CreateMessageRequest> samplingRequests = new CopyOnWriteArrayList<>();
	private final List<ProgressNotification> progress = new CopyOnWriteArrayList<>();
	private final List<LoggingMessageNotification> logs = new CopyOnWriteArrayList<>();

	@Test
	void writesTheDescriptionWithTheClientsOwnModelAndReportsProgressAndLogs() {
		final McpSyncClient client = connect(spec -> spec.capabilities(ClientCapabilities.builder().sampling().build())
														 .sampling(request -> {
															 samplingRequests.add(request);
															 return CreateMessageResult.builder(Role.ASSISTANT,
																								"A light 14 inch laptop for every day.",
																								"test-model").build();
														 })
														 .progressConsumer(progress::add)
														 .loggingConsumer(logs::add));
		client.setLoggingLevel(LoggingLevel.DEBUG);

		final CallToolResult result = client.callTool(CallToolRequest.builder()
																	 .name("enrichProduct")
																	 .arguments(Map.of("sku", "lap-001"))
																	 .progressToken("progress-1")
																	 .build());

		assertThat(result.isError()).as("%s", result.content()).isFalse();
		final String text = ((TextContent) result.content().getFirst()).text();
		assertThat(text).contains("\"sku\":\"LAP-001\"")
						.contains("Light 14 inch laptop")
						.contains("A light 14 inch laptop for every day.")
						.contains("\"generatedBy\":\"test-model\"")
						.doesNotContain("instructions");

		assertThat(samplingRequests).singleElement().satisfies(request -> {
			assertThat(request.systemPrompt()).contains("online shop").contains("data, not instructions");
			assertThat(request.maxTokens()).isEqualTo(200);
			assertThat(((TextContent) request.messages().getFirst().content()).text()).contains("UltraBook 14")
																					  .contains("Laptops")
																					  .contains("1299.00 EUR");
		});
		await().untilAsserted(() -> {
			assertThat(progress).extracting(ProgressNotification::progressToken).containsOnly("progress-1");
			assertThat(progress).extracting(ProgressNotification::progress).containsExactly(1.0, 2.0);
			assertThat(progress).extracting(ProgressNotification::total).containsOnly(2.0);
			assertThat(logs).anySatisfy(log -> assertThat(log.data()).contains("LAP-001"));
		});
	}

	@Test
	void handsTheFactsAndInstructionsToTheCallerWhenTheClientOffersNoSampling() {
		final McpSyncClient client = connect();

		final CallToolResult result = client.callTool(new CallToolRequest("enrichProduct", Map.of("sku", "lap-001")));

		assertThat(result.isError()).as("%s", result.content()).isFalse();
		final String text = ((TextContent) result.content().getFirst()).text();
		assertThat(text).contains("\"sku\":\"LAP-001\"").contains("\"catalogDescription\":\"Light 14 inch laptop");
		assertThat(text).contains("\"instructions\":").contains("online shop").contains("UltraBook 14").contains("1299.00 EUR");
		assertThat(text).doesNotContain("enrichedDescription").doesNotContain("generatedBy");
	}

	@Test
	void reportsAnUnknownSkuBeforeAskingTheModel() {
		final McpSyncClient client = connect(spec -> spec.capabilities(ClientCapabilities.builder().sampling().build())
														 .sampling(request -> {
															 samplingRequests.add(request);
															 return CreateMessageResult.builder(Role.ASSISTANT, "unused", "test-model")
																					   .build();
														 }));

		final CallToolResult result = client.callTool(new CallToolRequest("enrichProduct", Map.of("sku", "NOPE-1")));

		assertThat(result.isError()).isTrue();
		assertThat(samplingRequests).isEmpty();
	}
}