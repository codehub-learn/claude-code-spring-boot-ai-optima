package gr.codelearn.springbootai.persistencedemo.config.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Spring Data JPA wiring for the module.
 *
 * <p><b>Auditing.</b> Without {@code @EnableJpaAuditing} the {@code AuditingEntityListener} registered on
 * {@code common.AbstractEntity} never runs, and {@code @CreatedDate} / {@code @LastModifiedDate} stay null (the {@code NOT NULL}
 * columns would only be saved by their DB {@code systimestamp} default on raw inserts, not on Hibernate flushes). No {@code AuditorAware}
 * bean: the entities audit timestamps only, not "created by".
 */
@Configuration
@EnableJpaAuditing
class PersistenceConfig {
}