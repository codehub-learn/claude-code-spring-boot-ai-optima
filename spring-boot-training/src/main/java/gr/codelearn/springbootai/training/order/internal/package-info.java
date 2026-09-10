/**
 * Feature-private building blocks for the order use cases, kept out of the {@code order} root so only {@code OrderService} sits there:
 *
 * <ul>
 *   <li>{@code OrderFactory} — turns a {@code PlaceOrderCommand} into a persistent {@code Order} aggregate (resolves the customer and
 *       prices each line from {@code catalog}'s current {@code Product});</li>
 *   <li>{@code OrderPlacementService} — one {@code @Transactional(REQUIRES_NEW)} method (the U3.4 propagation exhibit);</li>
 *   <li>{@code OrderNotifications} — the {@code @TransactionalEventListener(AFTER_COMMIT)} side-effect stub;</li>
 *   <li>{@code PlaceOrderCommand} / {@code OrderConfirmation} / {@code OrderPlacedEvent} — the service's command, result and event
 *   types;</li>
 *   <li>{@code LowStockException} / {@code ReceiptDeliveryException} — the exceptions the U3.4 {@code rollbackFor} / {@code noRollbackFor}
 *       demos hang on.</li>
 * </ul>
 *
 * <p>Public within {@code order} so its sibling packages can use them; no package outside {@code order} imports them.
 */
@NullMarked
package gr.codelearn.springbootai.training.order.internal;

import org.jspecify.annotations.NullMarked;