package gr.codelearn.springbootai.training.catalog;

import java.math.BigDecimal;

/**
 * Interface projection over an <b>aggregate</b> JPQL query ({@link OrderRepository#summariseByStatus()}): one row per {@link OrderStatus}
 * with its order count and the sum of the cached {@code totalAmount}. The query aliases each {@code select} expression to the property name
 * ({@code count(o) as orderCount}), which is how Spring Data binds a non-entity tuple to the getters below.
 */
interface OrderStatusSummary {

	OrderStatus getStatus();

	long getOrderCount();

	BigDecimal getTotalValue();
}
