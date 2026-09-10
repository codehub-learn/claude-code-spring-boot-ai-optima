package gr.codelearn.springbootai.training.reporting;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

/**
 * Spring Data JDBC repository for the {@link ReportSnapshot} aggregate. {@link ListCrudRepository} gives {@code save} / {@code findById} /
 * {@code findAll} / {@code deleteById} returning {@code List} rather than {@code Iterable}; every operation acts on the whole aggregate
 * (root + {@code lines}).
 *
 * <p>{@code findByLabel} is a <b>derived query</b> - Spring Data JDBC parses the method name into
 * {@code select ... where label = ?}, then issues one extra {@code select} per returned root to fill its {@code lines} collection (fine
 * here: a snapshot has a handful of lines and there is one snapshot per label).
 *
 * <p>No {@code @EnableJdbcRepositories}: Boot's auto-configuration scans the application package, and Spring
 * Data's strict multi-store detection claims this interface for JDBC (its aggregate is {@code @Table}, it does not extend
 * {@code JpaRepository}) and leaves the {@code catalog} {@code JpaRepository} interfaces to JPA.
 */
interface SalesReportRepository extends ListCrudRepository<ReportSnapshot, Long> {

	Optional<ReportSnapshot> findByLabel(String label);
}