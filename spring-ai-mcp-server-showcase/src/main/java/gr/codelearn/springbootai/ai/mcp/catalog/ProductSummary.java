package gr.codelearn.springbootai.ai.mcp.catalog;

import java.math.BigDecimal;

/**
 * One row of a product search: enough to pick from, without the long description.
 */
public record ProductSummary(String sku, String name, String category, BigDecimal price) {
}