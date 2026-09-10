package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A product category — the "one" side of {@code category 1&#8211;&lt; product}. The {@code products} collection is the <b>inverse</b> side
 * of that association ({@code mappedBy = "category"}): {@link Product#getCategory()} owns the foreign key, so Hibernate reads this
 * collection but never writes a join from it. No cascade to delete — removing a category must not remove its products.
 *
 * <p>Business key: {@code name}. It has no setter, so {@code equals}/{@code hashCode} stay stable for the life of the instance (a mutable
 * key in a {@code HashSet} is a classic bug).
 */
@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Category extends AbstractAuditableEntity {

	@Column(nullable = false, unique = true)
	private String name;

	@Setter
	@Column
	private @Nullable String description;

	@OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private final Set<Product> products = new LinkedHashSet<>();

	Category(final String name, final @Nullable String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * Adds a product and fixes the owning side of the association in the same call, so an in-memory graph stays consistent without a
	 * reload. The inverse collection is otherwise read-only; {@link Product#setCategory(Category)} is what actually moves the foreign key.
	 */
	void addProduct(final Product product) {
		products.add(product);
		product.setCategory(this);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Category other)) {
			return false;
		}
		return Objects.equals(name, other.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Category{id=%d, name='%s'}".formatted(getId(), name);
	}
}