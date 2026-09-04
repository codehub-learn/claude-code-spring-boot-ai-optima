package gr.codelearn.springbootai.training.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Strongly typed, validated view of the {@code app.*} configuration tree. Bound by {@code @ConfigurationPropertiesScan} on
 * {@link gr.codelearn.springbootai.training.TrainingApplication}.
 *
 * <p>Conventions this class demonstrates (topic 1.3):
 * <ul>
 *   <li>a {@code record}, not a class with setters: config is immutable once the context is up;</li>
 *   <li>{@code @Validated} plus Jakarta constraints, so a bad value fails fast at startup with a readable report
 *       rather than surfacing as an NPE later;</li>
 *   <li>{@code @DefaultValue} on the nested {@link Catalog} parameter, so the whole {@code app.catalog} block can
 *       be omitted and still bind from the per-field defaults;</li>
 *   <li>a {@code @Nullable} optional key ({@code description}) that is genuinely allowed to be absent.</li>
 * </ul>
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
		@NotBlank String name,
		@Nullable String description,
		@DefaultValue @Valid Catalog catalog,
		@NotNull @Valid Cors cors) {

	/**
	 * Paging guard-rails for catalog listing endpoints (used from U3/U4).
	 */
	public record Catalog(
			@DefaultValue("20") @Positive @Max(200) int defaultPageSize,
			@DefaultValue("100") @Positive @Max(500) int maxPageSize,
			@DefaultValue("60s") Duration cacheTtl) {
	}

	/**
	 * CORS allow-list for the REST API; consumed by the web config in U4.
	 */
	public record Cors(
			@NotEmpty List<@NotBlank String> allowedOrigins,
			@DefaultValue("false") boolean allowCredentials) {
	}
}