package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A sellable product — the owning side of {@code product &gt;&#8211;1 category}. {@code @ManyToOne(optional = false)} makes the
 * {@code category_id} foreign key {@code NOT NULL}; {@code FetchType.LAZY} (the default is EAGER for {@code @ManyToOne}, overridden here on
 * purpose) means the category is fetched only when first touched, so a product list does not drag every category with it.
 *
 * <p>Business key: {@code sku}. No setter — see {@link Category} for why the key is immutable.
 */
@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Product extends AbstractAuditableEntity {

	@Setter
	@Column(nullable = false)
	private String name;

	@Setter
	@Column
	private @Nullable String description;

	@Column(nullable = false, unique = true)
	private String sku;

	@Setter
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Setter
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	Product(final String sku, final String name, final BigDecimal price, final Category category) {
		this.sku = sku;
		this.name = name;
		this.price = price;
		this.category = category;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Product other)) {
			return false;
		}
		return Objects.equals(sku, other.getSku());
	}

	@Override
	public int hashCode() {
		return Objects.hash(sku);
	}

	@Override
	public String toString() {
		return "Product{id=%d, sku='%s', name='%s'}".formatted(getId(), sku, name);
	}
}
