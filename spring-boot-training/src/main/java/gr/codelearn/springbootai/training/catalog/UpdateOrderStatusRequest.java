package gr.codelearn.springbootai.training.catalog;

/**
 * Request body for {@code PATCH /api/v1/orders/{orderNumber}/status}. A status transition is a partial update of one field, so it is a
 * PATCH on a {@code /status} sub-resource — not a {@code POST /orders/{n}/cancel} verb in the URL (CLAUDE.md: "HTTP verbs for actions, not
 * verbs in the URL").
 */
record UpdateOrderStatusRequest(OrderStatus status) {
}