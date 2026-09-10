/**
 * Unit 3.2 — the Catalog &amp; Orders domain model: the JPA entities, their associations, the {@link Address} embeddable and the
 * {@link OrderStatus} enum. The wider curriculum treats this as a single vertical slice that carries units 3 and 4 (persistence and REST);
 * repositories, queries and services follow in 3.3+.
 *
 * <p>What lives here:
 * <ul>
 *   <li>{@link AbstractAuditableEntity} — a {@code @MappedSuperclass} giving every entity an identity
 *       primary key, {@code @Version} optimistic locking and {@code @CreatedDate}/{@code @LastModifiedDate}
 *       auditing (activated by {@code config.persistence.PersistenceConfig});</li>
 *   <li>{@link Category} 1&#8211;&lt; {@link Product} — {@code @ManyToOne} plus the inverse
 *       {@code @OneToMany};</li>
 *   <li>{@link Customer} 1&#8211;&lt; {@link Order} 1&#8211;&lt; {@link OrderItem} &gt;&#8211;1
 *       {@link Product} — {@code Order}/{@code Product} is a many-to-many resolved through the
 *       {@code OrderItem} join entity, which carries {@code quantity} and {@code unitPrice};</li>
 *   <li>{@link Address} — an {@code @Embeddable} value object embedded twice ({@code Customer} billing,
 *       {@code Order} shipping) with {@code @AttributeOverrides}.</li>
 * </ul>
 *
 * <p>Entities are the one place Lombok is allowed (CLAUDE.md): {@code @Getter}/{@code @Setter} on the
 * mutable fields and a protected no-args constructor; {@code equals}/{@code hashCode} are hand-written
 * against the business key, never the generated id.
 */
@NullMarked
package gr.codelearn.springbootai.training.catalog;

import org.jspecify.annotations.NullMarked;