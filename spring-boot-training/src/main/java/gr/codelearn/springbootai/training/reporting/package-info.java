/**
 * Unit 3.5 - the reporting read-model slice: the non-JPA persistence options in the Spring stack, next to the JPA-heavy {@code catalog}
 * slice (3.2-3.4).
 *
 * <p>What lives here:
 * <ul>
 *   <li>{@link gr.codelearn.springbootai.training.reporting.CategorySalesQuery} - a read-only aggregate query
 *       over the {@code sales_ledger} fact table, mapped with {@code JdbcClient} (hand-written SQL, record
 *       result, no entity);</li>
 *   <li>{@link gr.codelearn.springbootai.training.reporting.ReportSnapshot} /
 *       {@link gr.codelearn.springbootai.training.reporting.ReportSnapshotLine} - a small <b>Spring Data JDBC</b>
 *       aggregate (root + owned children, loaded and stored whole) with
 *       {@link gr.codelearn.springbootai.training.reporting.SalesReportRepository};</li>
 *   <li>{@link gr.codelearn.springbootai.training.reporting.SalesIngestService} - a batch insert into
 *       {@code sales_ledger} via {@code NamedParameterJdbcTemplate} ({@code JdbcClient} has no batch API).</li>
 * </ul>
 *
 * <p>Spring Data JPA and Spring Data JDBC coexist with no repository configuration: Spring Data's strict
 * multi-store detection routes {@code @org.springframework.data.relational.core.mapping.Table} aggregates to
 * JDBC and {@code @jakarta.persistence.Entity} / {@code JpaRepository} to JPA. The aggregate types here use the
 * {@code org.springframework.data.*} annotations only - a stray {@code jakarta.persistence.*} import would
 * misroute the repository.
 */
@NullMarked
package gr.codelearn.springbootai.training.reporting;

import org.jspecify.annotations.NullMarked;