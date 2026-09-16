package gr.codelearn.springbootai.boot.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import org.jspecify.annotations.Nullable;

/**
 * Request body for {@code POST /api/v1/categories} (create) and {@code PUT /api/v1/categories/{name}} (replace). Mirrors
 * {@link ProductRequest}'s shape exactly: create and replace share every field except the business key. On create, {@code name} is the key
 * the client chooses; on replace it is immutable and comes from the path, so the body's {@code name} is ignored and only
 * {@code description} actually changes ({@link Category#setDescription}).
 *
 * <p><b>Validation groups.</b> {@code name} is required only in the {@link OnCreate} group. Both {@link OnCreate} and {@link OnUpdate}
 * {@code extend} {@link Default} — naming a group on {@code @Validated} replaces the implicit {@link Default} group rather than adding to
 * it, so without that {@code extends} nothing here would be checked at all past {@code name} (see {@link ProductRequest}'s Javadoc, and the
 * U4.2 lesson this mistake produced there first).
 */
public record CategoryRequest(@NotBlank(groups = OnCreate.class) String name, @Nullable String description) {

	/**
	 * Validation group applied to {@code POST /api/v1/categories}; extends {@link Default} so the ungrouped constraints still run.
	 */
	public interface OnCreate extends Default {
	}

	/**
	 * Validation group applied to {@code PUT /api/v1/categories/{name}}; extends {@link Default} for the same reason.
	 */
	public interface OnUpdate extends Default {
	}
}