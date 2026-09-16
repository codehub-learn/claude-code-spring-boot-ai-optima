package gr.codelearn.springbootai.boot.catalog.dto;

import gr.codelearn.springbootai.boot.catalog.Product;
import org.jspecify.annotations.Nullable;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * REST response view of a {@link Product} — a flat record, never the entity (CLAUDE.md: "Services never return entities to controllers").
 * {@code categoryName} flattens the {@code product → category} association so the client needs no second call.
 *
 * <p><b>Naming strategy (U4.3), scoped to this one record.</b> {@link JsonNaming} rewrites every field to {@code snake_case} on the wire
 * ({@code category_name}, not {@code categoryName}) — deliberately applied per-class rather than globally via
 * {@code spring.jackson.property-naming-strategy}: a global switch would silently reshape every other DTO's already-documented contract
 * (the order responses, the request bodies, …). Jackson 3 moved {@code PropertyNamingStrategies}/{@code @JsonNaming} to
 * {@code tools.jackson.databind}; the annotations module ({@code @JsonProperty}, {@code @JsonView}) stayed on
 * {@code com.fasterxml.jackson.annotation} — see {@code OrderDetailResponse} for both in use.
 *
 * <p>U4.1 kept this hand-built (via {@code CatalogWebMapper}); U4.4 moves the mapping to MapStruct.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProductResponse(String sku, String name, @Nullable String description, BigDecimal price, String categoryName) {
}