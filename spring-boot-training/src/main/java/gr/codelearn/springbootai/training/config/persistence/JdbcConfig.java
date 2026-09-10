package gr.codelearn.springbootai.training.config.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Spring Data JDBC wiring for the U3.5 {@code reporting} slice.
 *
 * <p>{@code @EnableJdbcRepositories} is here only to <b>scope the scan</b> to {@code reporting}. Without it,
 * Boot's auto-configured JDBC scanner considers every repository interface in the application package - including the {@code catalog}
 * {@code JpaRepository} interfaces - and logs a "Could not safely identify store assignment" line for each. {@code PersistenceConfig} pins
 * the JPA side to {@code catalog} the same way.
 *
 * <p>All the Spring Data JDBC infrastructure ({@code JdbcAggregateTemplate}, the Oracle dialect, converters)
 * still comes from Boot - this annotation only registers the repository config extension, so {@code SpringBootJdbcConfiguration} is
 * untouched. The store split itself is still enforced by Spring Data's strict multi-store detection ({@code @Table} vs {@code @Entity});
 * scoping just removes the startup noise.
 */
@Configuration
@EnableJdbcRepositories(basePackages = "gr.codelearn.springbootai.training.reporting")
class JdbcConfig {
}