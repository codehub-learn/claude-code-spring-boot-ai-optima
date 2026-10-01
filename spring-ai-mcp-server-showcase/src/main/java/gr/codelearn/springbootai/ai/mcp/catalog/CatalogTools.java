package gr.codelearn.springbootai.ai.mcp.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * The three catalog tools in Spring AI's {@code @Tool} style (12.1), published when {@code app.catalog.tool-style=tool};
 * {@link McpCatalogTools} is the same three in the MCP-native style. Read-only by construction, since the service only runs {@code select}.
 * A failure (an unknown SKU, a blank argument) is thrown as {@link IllegalArgumentException}; the MCP server returns its message to the
 * client as an error result the model can read and act on, not as a protocol failure.
 */
@Component
@ConditionalOnProperty(prefix = "app.catalog", name = "tool-style", havingValue = "tool")
public class CatalogTools {

	private final CatalogService service;

	CatalogTools(final CatalogService service) {
		this.service = service;
	}

	@Tool(description = "Searches the product catalog. Every filter is optional and filters combine: a text that must appear in the "
						+ "product name or description, a category name (see listCategories), and a maximum price in EUR. Returns up to "
						+ "'limit' products as sku, name, category and price, ordered by name. Use getProduct for the full description.")
	public List<ProductSummary> searchProducts(
			@ToolParam(description = "Text to look for in the name or description, case-insensitive",
					   required = false) final @Nullable String query,
			@ToolParam(description = "Exact category name, case-insensitive, for example one returned by listCategories",
					   required = false) final @Nullable String category,
			@ToolParam(description = "Highest price in EUR, inclusive", required = false) final @Nullable BigDecimal maxPrice,
			@ToolParam(description = "How many products to return at most; a default applies when omitted and a maximum is enforced",
					   required = false) final @Nullable Integer limit) {
		return service.search(query, category, maxPrice, limit);
	}

	@Tool(description = "Returns one product in full (sku, name, category, price and description) by its SKU, which is case-insensitive. "
						+ "Find SKUs with searchProducts.")
	public ProductDetail getProduct(
			@ToolParam(description = "The product SKU, for example as returned by searchProducts") final String sku) {
		return service.get(sku);
	}

	@Tool(description = "Lists every product category with the number of products in it. Use it to learn the valid category names for " +
						"searchProducts.")
	public List<CategorySummary> listCategories() {
		return service.categories();
	}
}