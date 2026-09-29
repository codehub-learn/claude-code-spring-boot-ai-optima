package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Unit 11.1: what Spring AI 2.0.1 makes of {@link CatalogTools}: the tool definitions the model receives, and how a tool call's JSON
 * arguments reach the Java method and its result goes back as JSON. No model, no Spring context.
 */
class CatalogToolsTest {

	private static final JsonMapper JSON = JsonMapper.shared();
	private static final ProductResponse LAPTOP =
			new ProductResponse("LAP-0003", "Flex 13 Convertible", "Laptops", new BigDecimal("1099.00"), "EUR", "13\" 2-in-1");

	private final ProductService productService = mock(ProductService.class);
	private final Map<String, ToolCallback> tools = Arrays.stream(ToolCallbacks.from(new CatalogTools(productService)))
														  .collect(Collectors.toMap(tool -> tool.getToolDefinition().name(),
																					Function.identity()));

	@Test
	void eachToolMethodBecomesADefinitionWithItsDescriptionAndAParameterSchema() {
		assertThat(tools).containsOnlyKeys("searchProducts", "getProduct");

		final ToolDefinition search = tools.get("searchProducts").getToolDefinition();
		assertThat(search.description()).startsWith("Search the store's product catalog.");
		final JsonNode schema = JSON.readTree(search.inputSchema());
		assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrder("keywords", "maxPrice");
		assertThat(schema.get("properties").get("maxPrice").get("description").asString()).contains("Highest price in EUR");
		// required = false keeps them out of "required": the model may leave them out.
		assertThat(schema.path("required").isMissingNode() || schema.get("required").isEmpty()).isTrue();

		final JsonNode getSchema = JSON.readTree(tools.get("getProduct").getToolDefinition().inputSchema());
		assertThat(getSchema.get("required").values()).extracting(JsonNode::asString).containsExactly("sku");
	}

	@Test
	void aToolCallsJsonArgumentsAreBoundToTheMethodAndTheResultIsJson() {
		given(productService.search(any(), any(), any(Integer.class))).willReturn(List.of(LAPTOP));

		final String result = tools.get("searchProducts").call("{\"keywords\":\"laptop\",\"maxPrice\":1500}");

		then(productService).should().search("laptop", new BigDecimal("1500"), CatalogTools.MAX_RESULTS);
		assertThat(JSON.readTree(result).get(0).get("sku").asString()).isEqualTo("LAP-0003");
		assertThat(result).contains("\"price\":1099.00");
	}

	@Test
	void leftOutOptionalArgumentsArriveAsNull() {
		given(productService.search(any(), any(), any(Integer.class))).willReturn(List.of());

		assertThat(tools.get("searchProducts").call("{}")).isEqualTo("[]");

		then(productService).should().search(null, null, CatalogTools.MAX_RESULTS);
	}

	@Test
	void anExceptionLeavesTheToolWrappedWithItsMessage() {
		given(productService.get("AUD-9999")).willThrow(new ProductNotFoundException("AUD-9999"));

		// The tool calling manager turns this into the tool's result for the model (see AssistantApiIT), not an HTTP error.
		assertThatThrownBy(() -> tools.get("getProduct").call("{\"sku\":\"AUD-9999\"}"))
				.isInstanceOf(ToolExecutionException.class)
				.hasMessage("No product with SKU AUD-9999");
	}
}