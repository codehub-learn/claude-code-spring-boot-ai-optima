package gr.codelearn.springbootai.training.catalog;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link Order} aggregate root. Highlights queries that reach <b>across associations</b>, an {@link EntityGraph} that
 * loads the whole aggregate in one round trip, and an aggregate JPQL query bound to an {@link OrderStatusSummary} projection.
 *
 * <p>{@code OrderItem} has no repository of its own — it is part of the {@code Order} aggregate and is only ever reached through its root
 * (CLAUDE.md: "cross-feature access goes through the other feature's service", and within a feature through the aggregate root).
 */
interface OrderRepository extends JpaRepository<Order, Long> {

	/**
	 * {@code where order_number = ?1} — the business key.
	 */
	Optional<Order> findByOrderNumber(String orderNumber);

	/**
	 * Derived query traversing {@code order → customer}: {@code where customer.email = ?1}.
	 */
	List<Order> findByCustomerEmail(String email);

	/**
	 * {@code where status = ?1}.
	 */
	List<Order> findByStatus(OrderStatus status);

	/**
	 * {@code where status in ?1}, ordered by the caller-supplied {@link Sort}.
	 */
	List<Order> findByStatusIn(Collection<OrderStatus> statuses, Sort sort);

	/**
	 * Loads an order together with its {@code customer} and its {@code items} in a single select. Without the graph, reading
	 * {@code order.getItems()} or {@code order.getCustomer()} after the persistence context closes throws
	 * {@code LazyInitializationException} ({@code open-in-view} is off).
	 */
	@EntityGraph(attributePaths = {"customer", "items"})
	Optional<Order> findWithCustomerAndItemsByOrderNumber(String orderNumber);

	/**
	 * Aggregate JPQL: one row per {@link OrderStatus} with its count and the sum of {@code totalAmount}. Each {@code select} expression is
	 * aliased to the matching {@link OrderStatusSummary} getter name so Spring Data can bind the tuple.
	 */
	@Query("""
		   select o.status as status, count(o) as orderCount, sum(o.totalAmount) as totalValue
		   from Order o
		   group by o.status
		   """)
	List<OrderStatusSummary> summariseByStatus();
}