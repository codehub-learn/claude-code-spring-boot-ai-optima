/**
 * The <b>order</b> feature — placing orders and moving them through their lifecycle over {@code /api/v1/orders}, plus the customers who
 * place them. Carries unit 3.4 (declarative-transaction semantics, the {@code OrderService} exhibit) and unit 4 (REST) for the ordering
 * half of the domain; products and categories live in the sibling {@code catalog} feature.
 *
 * <p>Package-by-feature, flat (CLAUDE.md): the controller, service, repositories, the {@code Order} aggregate ({@code Order},
 * {@code OrderItem}), {@code Customer}, the {@code Address} embeddable, the {@code OrderStatus} enum and the {@code OrderStatusSummary}
 * projection all sit at this package root. Only three sub-packages:
 * <ul>
 *   <li>{@code dto} — the REST request/response records (public wire contract);</li>
 *   <li>{@code mapper} — record&#8596;record mapping between the wire contract and the internal command/result types;</li>
 *   <li>{@code internal} — the order use-case building blocks: {@code OrderFactory}, the {@code REQUIRES_NEW}
 *       {@code OrderPlacementService}, the {@code AFTER_COMMIT} {@code OrderNotifications} listener, the command/result/event records, and
 *       the transaction-demo exceptions.</li>
 * </ul>
 *
 * <p>{@code OrderController} and {@code OrderNotifications} are package-private; {@code OrderStatusSummary} too. Types a sub-package needs
 * (the {@code internal} building blocks use {@code OrderService}, {@code OrderRepository}, {@code Order}, …) are {@code public} — Java
 * package access is not hierarchical, so {@code internal} is a naming convention, not an enforced wall.
 *
 * <p><b>Dependencies.</b> On {@code common} (auditing base type, {@code NotFoundException}) and — one way, read only — on {@code catalog}:
 * {@code OrderItem} holds a {@code @ManyToOne Product} and {@code OrderFactory} reads {@code ProductRepository} to price a line at order
 * time. A proper module boundary would broker that through {@code CatalogService} and store a price snapshot; kept as a direct reference
 * here to stay close to the JPA material.
 */
@NullMarked
package gr.codelearn.springbootai.boot.order;

import org.jspecify.annotations.NullMarked;