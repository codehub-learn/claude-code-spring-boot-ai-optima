package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Query-parameter command object for {@code GET /api/v1/products} — every field optional, any combination allowed. Spring MVC binds it from
 * {@code ?name=&category=&minPrice=&maxPrice=}; springdoc's {@code @ParameterObject} expands it into individual query parameters in Swagger
 * UI rather than a JSON blob.
 *
 * <p>{@link CatalogService} turns the non-null fields into composable {@link ProductSpecifications} fragments — the Criteria-API answer to
 * "a filter form where any field may be blank".
 */
record ProductFilter(
		@Nullable String name,
		@Nullable String category,
		@Nullable BigDecimal minPrice,
		@Nullable BigDecimal maxPrice) {
}
