package gr.codelearn.springbootai.training.config.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing. Without {@code @EnableJpaAuditing} the {@code AuditingEntityListener} registered on
 * {@code catalog.AbstractAuditableEntity} never runs, and {@code @CreatedDate} / {@code @LastModifiedDate} stay null (the {@code NOT NULL}
 * columns would then fall back to their DB {@code systimestamp} default, but only on raw inserts — not on Hibernate flushes).
 *
 * <p>No {@code AuditorAware} bean: the entities audit timestamps only, not "created by". Add one here if a user principal is ever needed.
 */
@Configuration
@EnableJpaAuditing
class PersistenceConfig {
}
