/**
 * Shared kernel for the training feature modules — the few types more than one feature needs, kept in one place so the features do not
 * depend on each other just to reach them.
 *
 * <ul>
 *   <li>{@link gr.codelearn.springbootai.boot.common.AbstractAuditableEntity} — the {@code @MappedSuperclass} that gives every JPA
 *       entity in {@code catalog} and {@code order} its identity key, {@code @Version} optimistic lock and
 *       {@code @CreatedDate}/{@code @LastModifiedDate} auditing (auditing is switched on by {@code config.persistence.PersistenceConfig}
 *       );</li>
 *   <li>{@link gr.codelearn.springbootai.boot.common.NotFoundException} — the "referenced row does not exist" exception thrown by both
 *       feature services; carries a {@code @ResponseStatus(NOT_FOUND)} stopgap until the central {@code ProblemDetail} advice (U4.2).</li>
 * </ul>
 *
 * <p>Nothing here depends on a feature package; the dependency arrow only ever points <i>into</i> {@code common}.
 */
@NullMarked
package gr.codelearn.springbootai.boot.common;

import org.jspecify.annotations.NullMarked;