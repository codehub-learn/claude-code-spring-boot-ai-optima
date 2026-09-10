package gr.codelearn.springbootai.training.catalog;

import java.math.BigDecimal;

/**
 * REST response view of an {@link Order}. U4.1 mirrors {@link OrderConfirmation} (number, status, total, line count) for every order
 * endpoint; U4.3 gives {@code GET /api/v1/orders/{orderNumber}} a richer {@code OrderDetailResponse} with the nested lines and a
 * {@code @JsonView} split between the list and detail shapes.
 */
record OrderResponse(String orderNumber, OrderStatus status, BigDecimal total, int lineCount) {
}
