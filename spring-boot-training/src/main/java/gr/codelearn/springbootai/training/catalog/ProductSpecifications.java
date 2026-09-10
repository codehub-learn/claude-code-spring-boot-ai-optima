package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Reusable {@link Specification} fragments for {@link Product}, the Criteria-API answer to "a filter form where any field may be blank".
 * Each factory returns a one-predicate {@code Specification}; the caller composes the non-blank ones with
 * {@link Specification#allOf(Specification[])} and hands the result to
 * {@link ProductRepository#findAll(Specification, org.springframework.data.domain.Pageable)}.
 *
 * <p>An absent argument yields {@link Specification#unrestricted()} (a no-op predicate) rather than {@code null}, so composition never has
 * to null-check. This keeps the dynamic-query logic here, out of the repository and out of the caller.
 */
final class ProductSpecifications {

	private ProductSpecifications() {
	}

	/**
	 * {@code lower(name) like %fragment%}, or a no-op when {@code fragment} is null/blank.
	 */
	static Specification<Product> nameContains(final @Nullable String fragment) {
		if (fragment == null || fragment.isBlank()) {
			return Specification.unrestricted();
		}
		final String pattern = "%" + fragment.toLowerCase(Locale.ROOT) + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
	}

	/**
	 * {@code price >= min}, or a no-op when {@code min} is null.
	 */
	static Specification<Product> priceAtLeast(final @Nullable BigDecimal min) {
		if (min == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
	}

	/**
	 * {@code price <= max}, or a no-op when {@code max} is null.
	 */
	static Specification<Product> priceAtMost(final @Nullable BigDecimal max) {
		if (max == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
	}

	/**
	 * {@code category = ?}, or a no-op when {@code category} is null. Uses an equality predicate on the {@code @ManyToOne}, not a join.
	 */
	static Specification<Product> inCategory(final @Nullable Category category) {
		if (category == null) {
			return Specification.unrestricted();
		}
		return (root, query, cb) -> cb.equal(root.get("category"), category);
	}
}