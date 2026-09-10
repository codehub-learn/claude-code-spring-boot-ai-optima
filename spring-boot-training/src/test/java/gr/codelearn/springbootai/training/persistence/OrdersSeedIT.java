package gr.codelearn.springbootai.training.persistence;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.2 counterpart to {@link CatalogSeedIT}: {@code R__020_seed_orders.sql} is a repeatable Flyway migration in {@code db/migration}, so
 * the {@code test} profile applies it after {@code V2}. Proves the baseline orders fixture is present and consistent, and that the
 * demo-only bulk ({@code db/demo/R__920_demo_orders.sql}) never leaks into the test database.
 *
 * <p>{@code order} is a reserved word and Oracle folds unquoted identifiers to upper case, so the table is referenced as {@code "order"}
 * (quoted, lower-case) in every query here.
 */
class OrdersSeedIT extends AbstractOracleIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void baselineCustomersAndOrdersAreSeeded() {
		final Long customers = jdbcClient.sql("select count(*) from customer").query(Long.class).single();
		final Long orders = jdbcClient.sql("select count(*) from \"order\"").query(Long.class).single();

		assertThat(customers).isGreaterThanOrEqualTo(3L);
		assertThat(orders).isGreaterThanOrEqualTo(3L);
	}

	@Test
	void everyOrderItemLinksARealOrderAndProduct() {
		final Long dangling = jdbcClient.sql("""
											 select count(*) from order_item oi
											 where not exists (select 1 from "order" o where o.id = oi.order_id)
											 or not exists (select 1 from product p where p.id = oi.product_id)
											 """)
										.query(Long.class)
										.single();

		assertThat(dangling).isZero();
	}

	@Test
	void seededOrderTotalsMatchTheirLines() {
		final Long mismatches = jdbcClient.sql("""
											   select count(*) from "order" o
											   where o.order_number like 'SEED-%'
											   and o.total_amount <> (select nvl(sum(oi.quantity * oi.unit_price), 0)
											   			  from order_item oi where oi.order_id = o.id)
											   """)
										  .query(Long.class)
										  .single();

		assertThat(mismatches).isZero();
	}

	@Test
	void demoBulkOrdersAreNotLoadedInTests() {
		final Long generated = jdbcClient.sql("select count(*) from \"order\" where order_number like 'GEN-%'")
										 .query(Long.class)
										 .single();

		assertThat(generated).isZero();
	}
}