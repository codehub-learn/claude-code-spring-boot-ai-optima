/**
 * The Dromeas courier delivery domain model: the JPA entities, their associations, the {@link Address} embeddable and the status enums.
 * Repositories, queries and services follow in later steps.
 *
 * <p>What lives here:
 * <ul>
 *   <li>{@link AbstractEntity} and {@link AbstractAuditableEntity}: {@code @MappedSuperclass} bases giving every entity an identity
 *       primary key and {@code created_at}; mutable ones add {@code updated_at} and {@code @Version} optimistic locking (auditing is
 *       activated by {@code config.persistence.PersistenceConfig});</li>
 *   <li>{@link Depot} 1&#8211;&lt; {@link Courier}, and {@link Depot} 1&#8211;&lt; {@link Customer};</li>
 *   <li>{@link Customer} 1&#8211;&lt; {@link Invoice}, and {@link Customer} 1&#8211;&lt; {@link Shipment};</li>
 *   <li>{@link Shipment} references its origin and destination {@link Depot} (two {@code @ManyToOne} to the same entity), an optional
 *       {@link Courier} and an optional {@link Invoice}, and owns its scan history: {@link Shipment} 1&#8211;&lt;
 *       {@link TrackingEvent} (the only inverse {@code @OneToMany}; the other collections would be huge, so they are queried);</li>
 *   <li>{@link Address}: an {@code @Embeddable} record embedded in {@link Customer} and, with {@code @AttributeOverrides}, in
 *       {@link Shipment} as the recipient address.</li>
 * </ul>
 *
 * <p>Entities are the one place Lombok is allowed (CLAUDE.md): {@code @Getter}/{@code @Setter} on the mutable fields and a protected
 * no-args constructor; {@code equals}/{@code hashCode} are hand-written against the business key, never the generated id.
 */
@NullMarked
package gr.codelearn.springbootai.persistencedemo.delivery;

import org.jspecify.annotations.NullMarked;