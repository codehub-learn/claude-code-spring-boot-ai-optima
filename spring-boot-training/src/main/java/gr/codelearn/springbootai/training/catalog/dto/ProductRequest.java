package gr.codelearn.springbootai.training.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/v1/products} (create) and {@code PUT /api/v1/products/{sku}} (replace). The {@code categoryName} must
 * match an existing {@link Category}; an unknown one is a {@link NotFoundException}.
 *
 * <p><b>Validation groups (U4.2).</b> Create and replace share every field except {@code sku}: on create it is the business key the
 * client chooses; on replace it is immutable and comes from the path, so the body's {@code sku} is ignored. Rather than two near-identical
 * records, {@code sku} is required only in the {@link OnCreate} group — {@code CatalogController} validates {@code createProduct} against
 * {@link OnCreate} and {@code replaceProduct} against {@link OnUpdate}. Every other constraint here carries no explicit group, which puts
 * it in {@link Default} — and both {@link OnCreate} and {@link OnUpdate} <b>extend</b> {@link Default} on purpose: specifying a group on
 * {@code @Validated} replaces the {@link Default} group rather than adding to it, so without that {@code extends} the unqualified
 * constraints (blank {@code name}, non-positive {@code price}, …) would silently stop being checked.
 */
public record ProductRequest(
		@NotBlank(groups = OnCreate.class) String sku,
		@NotBlank String name,
		@Nullable String description,
		@Positive BigDecimal price,
		@NotBlank String categoryName) {

	/**
	 * Validation group applied to {@code POST /api/v1/products}; extends {@link Default} so the ungrouped constraints still run.
	 */
	public interface OnCreate extends Default {
	}

	/**
	 * Validation group applied to {@code PUT /api/v1/products/{sku}}; extends {@link Default} for the same reason.
	 */
	public interface OnUpdate extends Default {
	}
}