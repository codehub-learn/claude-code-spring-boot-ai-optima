package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A customer order and the aggregate root of {@code order 1&#8211;&lt; order_item}. {@code order} is a reserved word in SQL, so the table
 * name is backtick-quoted — Hibernate emits it as {@code "order"} on Oracle, and the Flyway scripts quote it the same way.
 *
 * <p>Associations:
 * <ul>
 *   <li>{@code customer} — {@code @ManyToOne(optional = false, LAZY)}, the {@code customer_id} foreign key;</li>
 *   <li>{@code items} — {@code @OneToMany(mappedBy = "order")} with {@code cascade = ALL} and {@code orphanRemoval = true}: persisting or
 *       deleting the order cascades to its lines, and dropping a line from this set deletes its row. {@link OrderItem} owns the foreign
 *       key.</li>
 * </ul>
 * Together with {@link OrderItem#getProduct()} this models a many-to-many between {@code order} and {@code product} through a join entity
 * that carries its own columns ({@code quantity}, {@code unitPrice}).
 *
 * <p>{@code shippingAddress} re-embeds {@link Address} with {@code @AttributeOverrides} onto {@code ship_*} columns, distinct from the
 * customer's billing address. Business key: {@code orderNumber} (no setter — see {@link Category}).
 */
@Getter
@Entity
@Table(name = "`order`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Order extends AbstractAuditableEntity {

	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.NEW;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Setter
	@Column(name = "placed_at", nullable = false)
	private OffsetDateTime placedAt;

	@Setter
	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "line1", column = @Column(name = "ship_line1", nullable = false)),
			@AttributeOverride(name = "line2", column = @Column(name = "ship_line2")),
			@AttributeOverride(name = "city", column = @Column(name = "ship_city", nullable = false)),
			@AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code", nullable = false)),
			@AttributeOverride(name = "country", column = @Column(name = "ship_country", nullable = false))
	})
	private Address shippingAddress;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private final Set<OrderItem> items = new LinkedHashSet<>();

	Order(final String orderNumber, final Customer customer, final OffsetDateTime placedAt, final Address shippingAddress) {
		this.orderNumber = orderNumber;
		this.customer = customer;
		this.placedAt = placedAt;
		this.shippingAddress = shippingAddress;
	}

	/**
	 * Read-only view of the lines. Callers mutate through {@link #addItem} / {@link #removeItem} so both sides of the association and the
	 * cached {@link #totalAmount} stay consistent.
	 */
	Set<OrderItem> getItems() {
		return Collections.unmodifiableSet(items);
	}

	/**
	 * Builds a line bound to this order, adds it, and refreshes {@link #totalAmount}. The line's {@code order} back-reference is set here,
	 * so the object graph is consistent before any flush.
	 */
	OrderItem addItem(final Product product, final int quantity, final BigDecimal unitPrice) {
		final OrderItem item = new OrderItem(this, product, quantity, unitPrice);
		items.add(item);
		recalculateTotal();
		return item;
	}

	void removeItem(final OrderItem item) {
		items.remove(item);
		recalculateTotal();
	}

	/**
	 * Recomputes {@link #totalAmount} from the current lines. Called by the mutators; also useful after editing a line's quantity.
	 */
	void recalculateTotal() {
		this.totalAmount = items.stream()
								.map(OrderItem::lineTotal)
								.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Order other)) {
			return false;
		}
		return Objects.equals(orderNumber, other.getOrderNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(orderNumber);
	}

	@Override
	public String toString() {
		return "Order{id=%d, orderNumber='%s', status=%s, items=%d}".formatted(getId(), orderNumber, status, items.size());
	}
}