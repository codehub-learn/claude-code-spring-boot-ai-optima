package gr.codelearn.springbootai.training.reporting;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.5: {@link SnapshotService} + {@link SalesReportRepository} - the Spring Data JDBC aggregate. Verifies that a snapshot round-trips
 * whole (root + every line in one {@code findByLabel}), that re-capturing under the same label updates in place and replaces the child
 * rows, and that deleting the root takes its lines with it.
 *
 * <p>{@code @Transactional}: the writes here roll back, so they never touch the committed {@code baseline}
 * snapshot the {@code R__030} seed installs. {@code test-snap} is a distinct label.
 */
@Transactional
class SnapshotServiceIT extends AbstractOracleIntegrationTest {

	private static final String LABEL = "test-snap";
	private static final LocalDate JANUARY = LocalDate.of(2026, 1, 1);
	private static final LocalDate FEBRUARY = LocalDate.of(2026, 2, 1);

	@Autowired
	private SnapshotService snapshotService;

	@Autowired
	private SalesReportRepository salesReportRepository;

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void captureFreezesTheReportAndItReloadsWhole() {
		final SnapshotView view = snapshotService.capture(LABEL, JANUARY);

		assertThat(view.id()).isPositive();
		assertThat(view.label()).isEqualTo(LABEL);
		assertThat(view.capturedAt()).isNotNull();
		assertThat(view.lineCount()).isEqualTo(6);

		final ReportSnapshot reloaded = salesReportRepository.findByLabel(LABEL).orElseThrow();
		assertThat(reloaded.id()).isEqualTo(view.id());
		assertThat(reloaded.lines()).hasSize(6)
									.anySatisfy(line -> {
										assertThat(line.categoryName()).isEqualTo("Laptops");
										assertThat(line.unitsSold()).isEqualTo(3);
										assertThat(line.grossRevenue()).isEqualByComparingTo("5697.00");
									});
	}

	@Test
	void recapturingUnderTheSameLabelUpdatesInPlaceAndReplacesLines() {
		final long id = snapshotService.capture(LABEL, JANUARY).id();

		final SnapshotView recaptured = snapshotService.capture(LABEL, FEBRUARY);

		assertThat(recaptured.id()).isEqualTo(id); // same row, not a second snapshot
		assertThat(salesReportRepository.findAll()).filteredOn(s -> LABEL.equals(s.label())).hasSize(1);

		final ReportSnapshot reloaded = salesReportRepository.findByLabel(LABEL).orElseThrow();
		assertThat(reloaded.lines()).anySatisfy(line -> {
			assertThat(line.categoryName()).isEqualTo("Laptops");
			assertThat(line.grossRevenue()).isEqualByComparingTo("4398.00"); // the February figure
		});
	}

	@Test
	void deletingTheRootDeletesItsLines() {
		final long id = snapshotService.capture(LABEL, JANUARY).id();

		salesReportRepository.deleteById(id);

		assertThat(salesReportRepository.findByLabel(LABEL)).isEmpty();
		final Long orphanLines = jdbcClient.sql("select count(*) from report_snapshot_line where snapshot_id = ?")
										   .param(id)
										   .query(Long.class)
										   .single();
		assertThat(orphanLines).isZero();
	}
}