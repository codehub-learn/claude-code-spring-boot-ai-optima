package gr.codelearn.springbootai.training.catalog.dto;

import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Query-parameter command object for {@code GET /api/v1/products} — every field optional, any combination allowed. Spring MVC binds it from
 * {@code ?name=&category=&minPrice=&maxPrice=}; springdoc's {@code @ParameterObject} expands it into individual query parameters in Swagger
 * UI rather than a JSON blob.
 *
 * <p>{@link CatalogService} turns the non-null fields into composable {@link ProductSpecifications} fragments — the Criteria-API answer to
 * "a filter form where any field may be blank".
 *
 * <p>{@code @Valid} on the controller's {@code filter} parameter (U4.2) cascades into {@code @PositiveOrZero} here — a negative
 * {@code minPrice}/{@code maxPrice} fails validation before the query ever runs, the query-object counterpart to constraining a discrete
 * {@code @RequestParam} directly.
 */
public record ProductFilter(
		@Nullable String name,
		@Nullable String category,
		@Nullable @PositiveOrZero BigDecimal minPrice,
		@Nullable @PositiveOrZero BigDecimal maxPrice) {
}
