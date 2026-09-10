package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;

/**
 * Request body for {@code POST /api/v1/categories}. {@code name} is the business key; {@link CatalogService} rejects a duplicate. Bean
 * Validation lands in U4.2.
 */
record CreateCategoryRequest(String name, @Nullable String description) {
}
