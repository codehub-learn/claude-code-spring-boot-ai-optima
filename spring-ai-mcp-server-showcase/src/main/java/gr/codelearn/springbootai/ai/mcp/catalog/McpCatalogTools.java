package gr.codelearn.springbootai.ai.mcp.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * The same three catalog tools as {@link CatalogTools}, in the MCP-native style (unit 12.2), published when
 * {@code app.catalog.tool-style=mcp} (the default). Same names, descriptions and argument names, so a client cannot tell the styles apart
 * by the tool list, only by what the annotations add:
 *
 * <ul>
 *   <li>{@code @McpTool} carries MCP's tool <em>hints</em> ({@code readOnlyHint}, {@code destructiveHint}, {@code idempotentHint},
 *       {@code openWorldHint}) and a {@code title}; clients such as Claude Code use them to decide what needs a permission prompt.
 *       {@code @Tool} has no equivalent.</li>
 *   <li>{@code @McpToolParam.required} defaults to {@code true}, the opposite of {@code @ToolParam}, so optional arguments say
 *       {@code required = false} explicitly.</li>
 *   <li>It is discovered by the MCP annotation scanner ({@code spring.ai.mcp.server.annotation-scanner.enabled}), not through a
 *       {@code ToolCallbackProvider} bean.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "app.catalog", name = "tool-style", havingValue = "mcp", matchIfMissing = true)
public class McpCatalogTools {

	private final CatalogService service;

	McpCatalogTools(final CatalogService service) {
		this.service = service;
	}

	@McpTool(name = "searchProducts",
			 description = "Searches the product catalog. Every filter is optional and filters combine: a text that must appear in the "
						   + "product name or description, a category name (see listCategories), and a maximum price in EUR. Returns up " +
						   "to "
						   + "'limit' products as sku, name, category and price, ordered by name. Use getProduct for the full " +
						   "description.",
			 annotations = @McpTool.McpAnnotations(title = "Search products", readOnlyHint = true, destructiveHint = false,
												   idempotentHint = true, openWorldHint = false))
	public List<ProductSummary> searchProducts(
			@McpToolParam(description = "Text to look for in the name or description, case-insensitive",
						  required = false) final @Nullable String query,
			@McpToolParam(description = "Exact category name, case-insensitive, for example one returned by listCategories",
						  required = false) final @Nullable String category,
			@McpToolParam(description = "Highest price in EUR, inclusive", required = false) final @Nullable BigDecimal maxPrice,
			@McpToolParam(description = "How many products to return at most; a default applies when omitted and a maximum is enforced",
						  required = false) final @Nullable Integer limit) {
		return service.search(query, category, maxPrice, limit);
	}

	@McpTool(name = "getProduct",
			 description =
			         "Returns one product in full (sku, name, category, price and description) by its SKU, which is case-insensitive. "
					 + "Find SKUs with searchProducts.",
			 annotations = @McpTool.McpAnnotations(title = "Get a product", readOnlyHint = true, destructiveHint = false,
												   idempotentHint = true, openWorldHint = false))
	public ProductDetail getProduct(
			@McpToolParam(description = "The product SKU, for example as returned by searchProducts") final String sku) {
		return service.get(sku);
	}

	@McpTool(name = "listCategories",
			 description = "Lists every product category with the number of products in it. Use it to learn the valid category names for "
						   + "searchProducts.",
			 annotations = @McpTool.McpAnnotations(title = "List categories", readOnlyHint = true, destructiveHint = false,
												   idempotentHint = true, openWorldHint = false))
	public List<CategorySummary> listCategories() {
		return service.categories();
	}
}