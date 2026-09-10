package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * REST response view of a {@link Product} — a flat record, never the entity (CLAUDE.md: "Services never return entities to controllers").
 * {@code categoryName} flattens the {@code product → category} association so the client needs no second call.
 *
 * <p>U4.1 keeps this hand-built (via {@code CatalogWebMapper}); U4.3 revisits the shape (Jackson 3 tuning, {@code @JsonView}) and U4.4
 * moves the mapping to MapStruct.
 */
record ProductResponse(String sku, String name, @Nullable String description, BigDecimal price, String categoryName) {
}