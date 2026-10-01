package gr.codelearn.springbootai.ai.mcp.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * What {@code enrichProduct} returns, in one of two modes told apart by which fields are set (the unset ones are left out of the JSON).
 *
 * <ul>
 *   <li><b>Sampled</b> (the client offers MCP sampling): {@code enrichedDescription} is the text the client's model wrote and
 *       {@code generatedBy} names that model.</li>
 *   <li><b>For the caller</b> (no sampling, as in Claude Code): {@code instructions} holds the prompt, with the catalog facts inside, for
 *       the caller's own model to follow.</li>
 * </ul>
 *
 * @param catalogDescription  the description as it is in the catalog
 * @param enrichedDescription the text written through sampling, or {@code null}
 * @param generatedBy         the model the client reported for the sampling request, or {@code null}
 * @param instructions        what to do with the facts when no sampling was possible, or {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnrichedProduct(String sku, String name, @Nullable String catalogDescription, @Nullable String enrichedDescription,
							  @Nullable String generatedBy, @Nullable String instructions) {

	static EnrichedProduct sampled(final ProductDetail product, final String enrichedDescription, final String generatedBy) {
		return new EnrichedProduct(product.sku(), product.name(), product.description(), enrichedDescription, generatedBy, null);
	}

	static EnrichedProduct forCaller(final ProductDetail product, final String instructions) {
		return new EnrichedProduct(product.sku(), product.name(), product.description(), null, null, instructions);
	}
}
