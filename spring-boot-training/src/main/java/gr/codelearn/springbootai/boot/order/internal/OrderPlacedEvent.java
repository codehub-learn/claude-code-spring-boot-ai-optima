package gr.codelearn.springbootai.boot.order.internal;

import gr.codelearn.springbootai.boot.order.OrderService;

import java.math.BigDecimal;

/**
 * Published by {@link OrderService#placeOrder(PlaceOrderCommand)} once the order aggregate has been saved. A plain record — the in-process
 * event payload (CLAUDE.md: "records for event payloads").
 *
 * <p>{@link OrderNotifications} receives it with {@code @TransactionalEventListener(AFTER_COMMIT)}, so any side effect it triggers runs
 * only if the placing transaction actually committed.
 */
public record OrderPlacedEvent(String orderNumber, BigDecimal total) {
}