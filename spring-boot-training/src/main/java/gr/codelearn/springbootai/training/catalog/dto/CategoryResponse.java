package gr.codelearn.springbootai.training.catalog.dto;

import org.jspecify.annotations.Nullable;

/**
 * REST response view of a {@link Category}. The inverse {@code products} collection is deliberately not exposed here — a category listing
 * stays cheap, and products are their own paged resource.
 */
public record CategoryResponse(String name, @Nullable String description) {
}
