package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * U3.3: {@link OrderRepository} against the {@code R__020} baseline (3 customers; {@code SEED-0001} PAID, {@code SEED-0002} SHIPPED,
 * {@code SEED-0003} NEW). Covers association-traversing derived queries, the {@code @EntityGraph} that loads the aggregate in one shot
 * (contrasted with the {@code LazyInitializationException} you get without it), and the aggregate JPQL bound to
 * {@link OrderStatusSummary}.
 */
@Transactional
class OrderRepositoryIT extends AbstractOracleIntegrationTest {

	@Autowired
	private OrderRepository orderRepository;

	@PersistenceContext
	private EntityManager em;

	@Test
	void derivedQueriesTraverseAssociations() {
		assertThat(orderRepository.findByOrderNumber("SEED-0001")).isPresent();
		assertThat(orderRepository.findByCustomerEmail("alice@example.com"))
				.extracting(Order::getOrderNumber)
				.containsExactly("SEED-0001");
		assertThat(orderRepository.findByStatus(OrderStatus.PAID))
				.extracting(Order::getOrderNumber)
				.contains("SEED-0001");

		final List<Order> openOrShipped =
				orderRepository.findByStatusIn(List.of(OrderStatus.NEW, OrderStatus.SHIPPED), Sort.by("orderNumber"));
		assertThat(openOrShipped).extracting(Order::getOrderNumber).containsExactly("SEED-0002", "SEED-0003");
	}

	@Test
	void entityGraphLoadsCustomerAndItemsEagerly() {
		final Order order = orderRepository.findWithCustomerAndItemsByOrderNumber("SEED-0001").orElseThrow();
		em.clear(); // detach: only associations already fetched survive

		assertThat(order.getCustomer().getEmail()).isEqualTo("alice@example.com");
		assertThat(order.getItems()).hasSize(2);
	}

	@Test
	void withoutTheGraphLazyAccessFailsAfterTheContextCloses() {
		final Order order = orderRepository.findByOrderNumber("SEED-0001").orElseThrow();
		em.clear();

		assertThatThrownBy(() -> order.getItems().size())
				.isInstanceOf(org.hibernate.LazyInitializationException.class);
	}

	@Test
	void aggregateQueryBindsToInterfaceProjection() {
		final List<OrderStatusSummary> byStatus = orderRepository.summariseByStatus();

		assertThat(byStatus).extracting(OrderStatusSummary::getStatus)
							.contains(OrderStatus.NEW, OrderStatus.PAID, OrderStatus.SHIPPED);

		final OrderStatusSummary paid = byStatus.stream()
												.filter(s -> s.getStatus() == OrderStatus.PAID)
												.findFirst()
												.orElseThrow();
		assertThat(paid.getOrderCount()).isGreaterThanOrEqualTo(1);
		assertThat(paid.getTotalValue()).isGreaterThanOrEqualTo(new BigDecimal("1757.00"));
	}
}