package gr.codelearn.springbootai.training.persistence;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.5 counterpart to {@link CatalogSeedIT} / {@link OrdersSeedIT}: {@code R__030_seed_reporting.sql} is a repeatable Flyway migration in
 * {@code db/migration}, so the {@code test} profile applies it after {@code V3}. Proves the reporting baseline is present and internally
 * consistent - each {@code baseline} snapshot line equals the sum of its category's {@code SEED-L-} ledger rows. There is no demo-bulk tier
 * for reporting.
 */
class ReportingSeedIT extends AbstractOracleIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void baselineLedgerRowsAreSeeded() {
		final Long seeded = jdbcClient.sql("select count(*) from sales_ledger where ledger_ref like 'SEED-L-%'")
									  .query(Long.class)
									  .single();

		assertThat(seeded).isEqualTo(12L);
	}

	@Test
	void everySeededLedgerRowPointsAtARealProduct() {
		final Long orphans = jdbcClient.sql("""
											select count(*) from sales_ledger sl
											where sl.ledger_ref like 'SEED-L-%'
											and not exists (select 1 from product p where p.id = sl.product_id)
											""")
									   .query(Long.class)
									   .single();

		assertThat(orphans).isZero();
	}

	@Test
	void theBaselineSnapshotHasOneLinePerCategory() {
		final Long lines = jdbcClient.sql("""
										  select count(*) from report_snapshot_line rsl
										  join report_snapshot rs on rs.id = rsl.snapshot_id
										  where rs.label = 'baseline'
										  """)
									 .query(Long.class)
									 .single();

		assertThat(lines).isEqualTo(6L);
	}

	@Test
	void eachBaselineLineMatchesItsCategoryLedgerSum() {
		final Long mismatches = jdbcClient.sql("""
											   select count(*) from report_snapshot_line rsl
											   join report_snapshot rs on rs.id = rsl.snapshot_id and rs.label = 'baseline'
											   where rsl.gross_revenue <> (select nvl(sum(sl.net_amount), 0)
											   							  from sales_ledger sl
											   									   join product p on p.id = sl.product_id
											   									   join category c on c.id = p.category_id
											   							  where c.name = rsl.category_name
											   							  and sl.ledger_ref like 'SEED-L-%')
											   """)
										  .query(Long.class)
										  .single();

		assertThat(mismatches).isZero();
	}
}