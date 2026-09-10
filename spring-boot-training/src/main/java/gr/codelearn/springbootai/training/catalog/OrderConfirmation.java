package gr.codelearn.springbootai.training.catalog;

import java.math.BigDecimal;

/**
 * What {@link OrderService} hands back to a caller — a flat, immutable value, never the {@link Order} entity itself (CLAUDE.md: "Services
 * never return entities to controllers"). Built inside the service transaction, while the aggregate is still attached, so reading
 * {@code getItems().size()} here does not risk a {@code LazyInitializationException}.
 */
record OrderConfirmation(String orderNumber, OrderStatus status, BigDecimal total, int lineCount) {

	static OrderConfirmation from(final Order order) {
		return new OrderConfirmation(order.getOrderNumber(), order.getStatus(), order.getTotalAmount(), order.getItems().size());
	}
}
