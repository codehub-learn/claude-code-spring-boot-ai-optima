package gr.codelearn.springbootai.training.reporting;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.5: {@link SalesIngestService} batch insert against real Oracle. Deliberately <b>not</b> {@code @Transactional} - the point is that the
 * batch commits - so the rows are marked with an {@code IMP-} {@code ledger_ref} prefix and {@link #cleanUp()} deletes them after each
 * test.
 *
 * <p>The imported rows are dated 2026-06-01, after the {@code R__030} seed window (Jan/Feb), so
 * {@link CategorySalesQuery} aggregations over a June window see only what this test inserted.
 */
class SalesIngestServiceIT extends AbstractOracleIntegrationTest {

	private static final int BATCH_SIZE = 150;
	private static final LocalDate SOLD_ON = LocalDate.of(2026, 6, 1);

	@Autowired
	private SalesIngestService salesIngestService;

	@Autowired
	private CategorySalesQuery categorySalesQuery;

	@Autowired
	private JdbcClient jdbcClient;

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("delete from sales_ledger where ledger_ref like 'IMP-%'").update();
	}

	private long storageProductId() {
		return jdbcClient.sql("select id from product where sku = 'STO-0001'").query(Long.class).single();
	}

	@Test
	void ingestWritesEveryRowInOneBatch() {
		final long productId = storageProductId();
		final List<SalesLedgerEntry> entries = IntStream.range(0, BATCH_SIZE)
														.mapToObj(i -> new SalesLedgerEntry(
																"IMP-%04d".formatted(i), productId, SOLD_ON, 1,
																new BigDecimal("10.00")))
														.toList();

		final int written = salesIngestService.ingest(entries);

		assertThat(written).isEqualTo(BATCH_SIZE);
		final Long stored = jdbcClient.sql("select count(*) from sales_ledger where ledger_ref like 'IMP-%'")
									  .query(Long.class)
									  .single();
		assertThat(stored).isEqualTo(BATCH_SIZE);
	}

	@Test
	void ingestedRowsAreVisibleToTheAggregateQuery() {
		final long productId = storageProductId();
		salesIngestService.ingest(IntStream.range(0, BATCH_SIZE)
										   .mapToObj(i -> new SalesLedgerEntry(
												   "IMP-%04d".formatted(i), productId, SOLD_ON, 2, new BigDecimal("10.00")))
										   .toList());

		assertThat(categorySalesQuery.salesByCategory(SOLD_ON)).anySatisfy(row -> {
			assertThat(row.categoryName()).isEqualTo("Storage");
			assertThat(row.lineCount()).isEqualTo(BATCH_SIZE);
			assertThat(row.unitsSold()).isEqualTo(2L * BATCH_SIZE);
			assertThat(row.grossRevenue()).isEqualByComparingTo(new BigDecimal("1500.00"));
		});
	}

	@Test
	void anEmptyBatchWritesNothing() {
		assertThat(salesIngestService.ingest(List.of())).isZero();
	}
}