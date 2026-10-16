package gr.codelearn.springbootai.coredemo.order;

import gr.codelearn.springbootai.coredemo.customer.Address;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(long id, String orderNumber, long customerId, Address shippingAddress, List<OrderItem> items, OrderStatus status,
					BigDecimal totalAmount, Instant placedAt) {

	public Order {
		items = List.copyOf(items);
	}

	static Order place(long id, long customerId, Address shippingAddress, List<OrderItem> items, Instant placedAt) {
		var total = items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Order(id, "ORD-%06d".formatted(id), customerId, shippingAddress, items, OrderStatus.NEW, total, placedAt);
	}

	public Order withStatus(OrderStatus next) {
		if (!status.canTransitionTo(next)) {
			throw new InvalidOrderStateException(id, status, next);
		}
		return new Order(id, orderNumber, customerId, shippingAddress, items, next, totalAmount, placedAt);
	}
}
