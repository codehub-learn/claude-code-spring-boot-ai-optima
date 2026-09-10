package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/v1/products}. The {@code categoryName} must match an existing {@link Category}; an unknown one is a
 * {@link CatalogNotFoundException}.
 *
 * <p>No Bean Validation annotations yet — {@code @NotBlank} / {@code @Positive} and validation groups land in U4.2. For now
 * {@link CatalogService} guards against a duplicate {@code sku} and a missing category.
 */
record CreateProductRequest(String sku, String name, @Nullable String description, BigDecimal price, String categoryName) {
}
