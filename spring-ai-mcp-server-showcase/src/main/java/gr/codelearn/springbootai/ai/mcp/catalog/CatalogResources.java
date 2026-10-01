package gr.codelearn.springbootai.ai.mcp.catalog;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The catalog as MCP <em>resources</em> (unit 12.2). A tool is something the model decides to call, with arguments; a resource is content
 * addressed by a URI that the application or the user attaches to the conversation (Claude Code offers them as {@code @}-mentions). Same
 * data as {@code listCategories} and {@code getProduct}, reached the other way round.
 *
 * <p>A {@code {sku}} in the URI makes {@link #product} a resource <em>template</em>: it shows up under {@code resources/templates/list},
 * not
 * {@code resources/list}, and the method parameter named like the variable receives the value.
 */
@Component
public class CatalogResources {

	private final CatalogService service;
	private final JsonMapper jsonMapper;

	CatalogResources(final CatalogService service, final JsonMapper jsonMapper) {
		this.service = service;
		this.jsonMapper = jsonMapper;
	}

	@McpResource(uri = "catalog://categories", name = "categories",
				 description = "Every product category with the number of products in it, as JSON.", mimeType = "application/json")
	public String categories() {
		return jsonMapper.writeValueAsString(service.categories());
	}

	@McpResource(uri = "catalog://product/{sku}", name = "product",
				 description = "One product in full (sku, name, category, price, description) as JSON; the SKU is case-insensitive.",
				 mimeType = "application/json")
	public String product(final String sku) {
		return jsonMapper.writeValueAsString(service.get(sku));
	}
}