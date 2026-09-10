package gr.codelearn.springbootai.training.reporting;

import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * One line of a {@link ReportSnapshot}: the frozen sales figure for a single category. An <b>owned</b> child of the aggregate - it has no
 * {@code @Id} and no identity outside its snapshot. Spring Data JDBC writes the {@code snapshot_id} back-reference itself and, whenever the
 * root is saved, deletes every line for the snapshot and re-inserts the current set. The business key is
 * {@code (snapshot_id, category_name)}, enforced by a unique constraint in {@code V3}.
 *
 * <p>A Java record works fine as a Spring Data relational type; the canonical constructor is the persistence
 * creator. Uses the {@code org.springframework.data.relational} {@code @Table} - never {@code jakarta.persistence} - so the strict
 * multi-store detector routes the aggregate to Spring Data JDBC. The name is UPPER CASE for the same reason as {@link ReportSnapshot}:
 * Spring Data JDBC quotes the literal and Oracle folds unquoted DDL names upper case.
 */
@Table("REPORT_SNAPSHOT_LINE")
record ReportSnapshotLine(String categoryName, long unitsSold, BigDecimal grossRevenue) {
}