package gr.codelearn.springbootai.ai.mcp.catalog;

import org.jspecify.annotations.Nullable;

/**
 * What {@code enrichProduct} returns: the catalog text next to the text the client's model wrote from it.
 *
 * @param generatedBy the model the client reported for the sampling request
 */
public record EnrichedProduct(String sku, String name, @Nullable String catalogDescription, String enrichedDescription,
                              String generatedBy) {
}