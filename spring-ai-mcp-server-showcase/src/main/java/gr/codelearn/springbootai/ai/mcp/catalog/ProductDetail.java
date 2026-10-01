package gr.codelearn.springbootai.ai.mcp.catalog;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * One product in full, as returned by {@code getProduct}.
 */
public record ProductDetail(String sku, String name, String category, BigDecimal price, @Nullable String description) {
}