package gr.codelearn.springbootai.ai.product;

import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
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
import static org.mockito.Mockito.times;

/**
 * Unit 11.1 and 11.2: what Spring AI 2.0.1 makes of {@link CatalogTools}: the tool definitions the model receives, how a tool call's JSON
 * arguments reach the Java method and its result goes back as JSON, and how invalid arguments are rejected. No model, no Spring context:
 * the tools are wrapped in the same {@link MethodValidationInterceptor} that Spring Boot's method validation applies to a
 * {@code @Validated} bean.
 */
class CatalogToolsTest {

	private static final JsonMapper JSON = JsonMapper.shared();
	private static final ProductResponse LAPTOP =
			new ProductResponse("LAP-0003", "Flex 13 Convertible", "Laptops", new BigDecimal("1099.00"), "EUR", "13\" 2-in-1");

	private final ProductService productService = mock(ProductService.class);
	private final Map<String, ToolCallback> tools = Arrays.stream(ToolCallbacks.from(validated(new CatalogTools(productService))))
														  .collect(Collectors.toMap(tool -> tool.getToolDefinition().name(),
																					Function.identity()));

	private static CatalogTools validated(final CatalogTools target) {
		final ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvice(new MethodValidationInterceptor(Validation.buildDefaultValidatorFactory()));
		return (CatalogTools) factory.getProxy();
	}

	@Test
	void eachToolMethodBecomesADefinitionWithItsDescriptionAndAnInputSchema() {
		assertThat(tools).containsOnlyKeys("searchProducts", "getProduct");

		final ToolDefinition search = tools.get("searchProducts").getToolDefinition();
		assertThat(search.description()).startsWith("Search the store's product catalog.");
		final JsonNode input = JSON.readTree(search.inputSchema()).get("properties").get("input");
		assertThat(input.get("properties").propertyNames()).containsExactlyInAnyOrder("keywords", "maxPrice");
		assertThat(input.get("properties").get("maxPrice").get("description").asString()).contains("Highest price in EUR");
		// required = false keeps them out of "required": the model may leave them out.
		assertThat(input.path("required").isMissingNode() || input.get("required").isEmpty()).isTrue();

		final JsonNode getSchema = JSON.readTree(tools.get("getProduct").getToolDefinition().inputSchema());
		assertThat(getSchema.get("required").values()).extracting(JsonNode::asString).containsExactly("input");
	}

	@Test
	void aToolCallsJsonArgumentsAreBoundToTheMethodAndTheResultIsJson() {
		given(productService.search(any(), any(), any(Integer.class))).willReturn(List.of(LAPTOP));

		final String result = tools.get("searchProducts").call("{\"input\":{\"keywords\":\"laptop\",\"maxPrice\":1500}}");

		then(productService).should().search("laptop", new BigDecimal("1500"), CatalogTools.MAX_RESULTS);
		assertThat(JSON.readTree(result).get(0).get("sku").asString()).isEqualTo("LAP-0003");
		assertThat(result).contains("\"price\":1099.00");
	}

	@Test
	void leftOutOptionalArgumentsArriveAsNull() {
		given(productService.search(any(), any(), any(Integer.class))).willReturn(List.of());

		assertThat(tools.get("searchProducts").call("{\"input\":{}}")).isEqualTo("[]");
		assertThat(tools.get("searchProducts").call("{}")).isEqualTo("[]");

		then(productService).should(times(2)).search(null, null, CatalogTools.MAX_RESULTS);
	}

	@Test
	void anExceptionLeavesTheToolWrappedWithItsMessage() {
		given(productService.get("AUD-9999")).willThrow(new ProductNotFoundException("AUD-9999"));

		// The tool calling manager turns this into the tool's result for the model (see AssistantApiIT), not an HTTP error.
		assertThatThrownBy(() -> tools.get("getProduct").call("{\"input\":{\"sku\":\"AUD-9999\"}}"))
				.isInstanceOf(ToolExecutionException.class)
				.hasMessage("No product with SKU AUD-9999");
	}

	@Test
	void argumentsThatBreakAConstraintNeverReachTheService() {
		for (final String call : List.of("{\"input\":{\"sku\":\"the blue laptop\"}}", "{\"input\":{\"sku\":\"\"}}",
										 "{\"input\":{}}")) {
			assertThatThrownBy(() -> tools.get("getProduct").call(call)).isInstanceOf(ToolExecutionException.class)
																		.hasRootCauseInstanceOf(ConstraintViolationException.class);
		}
		assertThatThrownBy(() -> tools.get("searchProducts").call("{\"input\":{\"maxPrice\":-5}}"))
				.hasRootCauseInstanceOf(ConstraintViolationException.class)
				.rootCause()
				.hasMessageContaining("maxPrice: must be greater than 0");

		then(productService).shouldHaveNoInteractions();
	}

	@Test
	void aValidLookupPassesTheConstraints() {
		given(productService.get("LAP-0003")).willReturn(LAPTOP);

		assertThat(tools.get("getProduct").call("{\"input\":{\"sku\":\"LAP-0003\"}}")).contains("Flex 13 Convertible");
	}
}
