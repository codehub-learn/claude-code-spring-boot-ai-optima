package gr.codelearn.springbootai.training.reporting;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A <b>Spring Data JDBC aggregate</b>: a frozen sales-per-category report. {@code ReportSnapshot} is the aggregate root and
 * {@link ReportSnapshotLine} its owned children - Spring Data JDBC loads and stores the whole thing together. There is no lazy loading, no
 * dirty tracking, no first-level cache: {@code save} writes the root and replaces all child rows, {@code findById} returns the root with
 * every line populated.
 *
 * <p>This is the contrast to the {@code catalog} JPA entities. There: mutable classes, a persistence context, an
 * object graph you navigate lazily. Here: an immutable record, a hard aggregate boundary, one round of SQL to load it whole. Reach for
 * Spring Data JDBC when the aggregate is small and self-contained and you want SQL you can predict.
 *
 * <p>Annotations are {@code org.springframework.data.*} only ({@link Id} from
 * {@code org.springframework.data.annotation}, {@link Table}/{@link MappedCollection} from {@code org.springframework.data.relational}) - a
 * {@code jakarta.persistence} annotation here would make Spring Data's strict multi-store detector hand {@link SalesReportRepository} to
 * JPA instead.
 *
 * <p>The {@code @Table} / {@code idColumn} names are <b>UPPER CASE</b> on purpose. Spring Data JDBC renders a
 * string literal here as a quoted identifier verbatim; Oracle stores unquoted DDL names upper case, so a quoted {@code "report_snapshot"}
 * would not resolve (ORA-00942). Derived names (the record components -> {@code label} -> {@code LABEL}) are unquoted and fold correctly,
 * so only the literals need the casing.
 *
 * @param id         {@code null} until saved; Oracle {@code generated always as identity} fills it and Spring Data JDBC returns a new
 *                   record instance carrying the real key
 * @param label      the natural key ({@code uq_report_snapshot_label}); {@link SalesReportRepository#findByLabel}
 * @param fromDate   the start of the window the figures cover
 * @param capturedAt when the snapshot was taken - a plain {@link LocalDateTime} against a {@code timestamp} column, deliberately not
 *                   {@code timestamp with time zone} / {@code Instant} (see V3)
 * @param lines      the per-category figures; {@code snapshot_id} is the managed back-reference
 */
@Table("REPORT_SNAPSHOT")
record ReportSnapshot(
		@Id @Nullable Long id,
		String label,
		LocalDate fromDate,
		LocalDateTime capturedAt,
		@MappedCollection(idColumn = "SNAPSHOT_ID") Set<ReportSnapshotLine> lines) {

	/**
	 * Builds an unsaved snapshot ({@code id == null}) from a {@link CategorySalesQuery} result, stamped now.
	 */
	static ReportSnapshot of(final String label, final LocalDate fromDate, final List<CategorySales> sales) {
		return new ReportSnapshot(null, label, fromDate, LocalDateTime.now(), toLines(sales));
	}

	/**
	 * Re-stamps this snapshot with a fresh window, capture time and figures, keeping its {@code id} so a
	 * {@link SalesReportRepository#save}
	 * updates the existing row and replaces its lines.
	 */
	ReportSnapshot recapturedWith(final LocalDate fromDate, final List<CategorySales> sales) {
		return new ReportSnapshot(id, label, fromDate, LocalDateTime.now(), toLines(sales));
	}

	private static Set<ReportSnapshotLine> toLines(final List<CategorySales> sales) {
		return sales.stream()
					.map(s -> new ReportSnapshotLine(s.categoryName(), s.unitsSold(), s.grossRevenue()))
					.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}