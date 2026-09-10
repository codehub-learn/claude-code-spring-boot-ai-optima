package gr.codelearn.springbootai.training.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Freezes a {@link CategorySalesQuery} result into a persistent {@link ReportSnapshot} aggregate.
 *
 * <p>{@link #capture} is idempotent on the {@code label}: a first call inserts a new snapshot; a repeat call
 * loads the existing one and re-saves it with today's figures. Re-saving an aggregate whose {@code id} is already set is where Spring Data
 * JDBC's model shows: it updates the root row, then <b>deletes every {@code report_snapshot_line} for the snapshot and re-inserts the
 * current set</b> - the children have no independent lifecycle.
 *
 * <p>{@code @Transactional} on the service (never the repository). The read (JdbcClient) and the write (Spring
 * Data JDBC) share one transaction on the one {@code DataSource}.
 */
@Service
class SnapshotService {

	private final CategorySalesQuery categorySalesQuery;
	private final SalesReportRepository repository;

	SnapshotService(final CategorySalesQuery categorySalesQuery, final SalesReportRepository repository) {
		this.categorySalesQuery = categorySalesQuery;
		this.repository = repository;
	}

	@Transactional
	SnapshotView capture(final String label, final LocalDate since) {
		final List<CategorySales> sales = categorySalesQuery.salesByCategory(since);

		final ReportSnapshot toSave = repository.findByLabel(label)
												.map(existing -> existing.recapturedWith(since, sales))
												.orElseGet(() -> ReportSnapshot.of(label, since, sales));

		return SnapshotView.of(repository.save(toSave));
	}
}