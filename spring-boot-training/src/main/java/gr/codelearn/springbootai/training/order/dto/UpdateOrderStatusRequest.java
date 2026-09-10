package gr.codelearn.springbootai.training.order.dto;

import gr.codelearn.springbootai.training.order.OrderStatus;

/**
 * Request body for {@code PATCH /api/v1/orders/{orderNumber}/status}. A status transition is a partial update of one field, so it is a
 * PATCH on a {@code /status} sub-resource — not a {@code POST /orders/{n}/cancel} verb in the URL (CLAUDE.md: "HTTP verbs for actions, not
 * verbs in the URL").
 */
public record UpdateOrderStatusRequest(OrderStatus status) {
}
