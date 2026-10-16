package gr.codelearn.springbootai.coredemo.order;

import gr.codelearn.springbootai.coredemo.customer.Address;
import gr.codelearn.springbootai.coredemo.customer.AddressType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

	private static Order newOrder() {
		var address = new Address("1 Main St", "Athens", "10431", "GR", AddressType.SHIPPING);
		var items = List.of(new OrderItem(1, "Mug", 2, new BigDecimal("9.90")), new OrderItem(2, "Pen", 3, new BigDecimal("1.50")));
		return Order.place(7, 1, address, items, Instant.parse("2026-01-01T00:00:00Z"));
	}

	@Test
	void placeComputesTotalAndNumber() {
		var order = newOrder();

		assertThat(order.totalAmount()).isEqualByComparingTo("24.30");
		assertThat(order.orderNumber()).isEqualTo("ORD-000007");
		assertThat(order.status()).isEqualTo(OrderStatus.NEW);
	}

	@Test
	void validTransitionsSucceed() {
		var delivered = newOrder().withStatus(OrderStatus.PAID).withStatus(OrderStatus.SHIPPED).withStatus(OrderStatus.DELIVERED);

		assertThat(delivered.status()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void invalidTransitionFails() {
		assertThatThrownBy(() -> newOrder().withStatus(OrderStatus.SHIPPED)).isInstanceOf(InvalidOrderStateException.class);
	}
}
