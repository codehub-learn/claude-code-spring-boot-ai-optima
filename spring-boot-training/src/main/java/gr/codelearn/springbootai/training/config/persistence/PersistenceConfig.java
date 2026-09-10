package gr.codelearn.springbootai.training.config.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Data JPA wiring for the module.
 *
 * <p><b>Auditing.</b> Without {@code @EnableJpaAuditing} the {@code AuditingEntityListener} registered on
 * {@code catalog.AbstractAuditableEntity} never runs, and {@code @CreatedDate} / {@code @LastModifiedDate} stay null (the {@code NOT NULL}
 * columns would then fall back to their DB {@code systimestamp} default, but only on raw inserts — not on Hibernate flushes). No
 * {@code AuditorAware} bean: the entities audit timestamps only, not "created by". Add one here if a user principal is ever needed.
 *
 * <p><b>Repository scanning is scoped.</b> Since U3.5 the module runs two Spring Data modules (JPA for
 * {@code catalog}, JDBC for {@code reporting} — see {@code config.persistence.JdbcConfig}). Left to auto-configuration each scanner
 * considers <i>every</i> repository interface and logs a "Could not safely identify store assignment" line for the ones it cannot claim.
 * Pinning each module to its own {@code basePackages} keeps startup quiet and makes the split explicit. This replaces Boot's
 * {@code JpaRepositoriesAutoConfiguration}.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "gr.codelearn.springbootai.training.catalog")
class PersistenceConfig {
}