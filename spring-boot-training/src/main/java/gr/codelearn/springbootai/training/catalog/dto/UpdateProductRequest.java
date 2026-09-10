package gr.codelearn.springbootai.training.catalog.dto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Request body for {@code PUT /api/v1/products/{sku}} — a full replacement of the mutable fields (the {@code sku} business key is immutable
 * and comes from the path). A partial-update (PATCH / JSON Merge Patch) payload is a U4.3 topic.
 */
public record UpdateProductRequest(String name, @Nullable String description, BigDecimal price, String categoryName) {
}
