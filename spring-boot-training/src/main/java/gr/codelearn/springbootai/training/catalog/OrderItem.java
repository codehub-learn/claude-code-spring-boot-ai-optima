package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One line of an {@link Order}: a {@link Product}, a quantity and the unit price captured at order time (so later price changes do not
 * rewrite history). This is the <b>join entity</b> that turns the {@code order}/{@code product} many-to-many into two {@code @ManyToOne}s,
 * with room for its own columns. The {@code uq_order_item_order_product} constraint keeps a product from appearing twice on one order.
 *
 * <p>Business key: the ({@code order}, {@code product}) pair. Instances are created through
 * {@link Order#addItem(Product, int, BigDecimal)}, never directly.
 */
@Getter
@Entity
@Table(name = "order_item", uniqueConstraints =
@UniqueConstraint(name = "uq_order_item_order_product", columnNames = {"order_id", "product_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderItem extends AbstractAuditableEntity {

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Setter
	@Column(nullable = false)
	private int quantity;

	@Setter
	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	OrderItem(final Order order, final Product product, final int quantity, final BigDecimal unitPrice) {
		this.order = order;
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	/**
	 * {@code unitPrice * quantity}. Not a persisted column — {@link Order#recalculateTotal()} sums these to keep
	 * {@code order.total_amount}.
	 */
	BigDecimal lineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OrderItem other)) {
			return false;
		}
		return Objects.equals(order, other.getOrder()) && Objects.equals(product, other.getProduct());
	}

	@Override
	public int hashCode() {
		return Objects.hash(order, product);
	}

	@Override
	public String toString() {
		return "OrderItem{id=%d, product=%s, quantity=%d}".formatted(getId(), product, quantity);
	}
}