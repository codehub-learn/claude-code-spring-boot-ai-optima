package gr.codelearn.springbootai.boot.catalog.dto;

import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Request body for {@code PATCH /api/v1/products/{sku}} — every field optional; a {@code null} (or omitted) field is left unchanged,
 * anything else replaces the current value. Unlike {@link ProductRequest} this needs no validation groups: every constraint here already
 * ignores {@code null} (Jakarta Bean Validation's rule for everything except {@code @NotNull}/{@code @NotBlank}/{@code @NotEmpty}), so
 * "only validate what was actually provided" falls out of the constraint annotations themselves.
 *
 * <p><b>Known limitation.</b> Jackson binds a genuinely absent JSON field and an explicit {@code "field": null} to the same Java
 * {@code null} — a plain record cannot tell them apart. That means this PATCH can replace {@code description} or leave it alone, but it can
 * never clear it back to {@code null} once set; the client would have to fall back to {@code PUT} for that. RFC 7396 JSON Merge Patch (an
 * {@code application/merge-patch+json} body deserialized as a {@code JsonNode} and applied field-by-field, so "absent" and "null" are
 * distinguishable) or RFC 6902 JSON Patch are the correct tools when that distinction matters; deliberately out of scope for this teaching
 * example — the simple "non-null replaces" contract covers the common case and keeps the DTO a plain record.
 */
public record ProductPatchRequest(
		@Nullable String name,
		@Nullable String description,
		@Nullable @Positive BigDecimal price,
		@Nullable String categoryName) {
}