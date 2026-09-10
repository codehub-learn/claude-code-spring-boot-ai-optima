package gr.codelearn.springbootai.training.persistence;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.6: the persistence-testing techniques that need a full Testcontainers context rather than a slice.
 *
 * <ul>
 *   <li><b>Explicit rollback verification.</b> {@link #testManagedTransactionRollsBackSoTheRowNeverLands()} does not
 *       just <i>trust</i> that an {@code @Transactional} test rolls back - it drives {@link TestTransaction}
 *       manually: insert, end the transaction with a rollback, then prove in a fresh transaction that the row is
 *       gone.</li>
 *   <li><b>{@code @Sql} bracketing a non-transactional test.</b> {@link #sqlScriptsBracketANonTransactionalTest()}
 *       has no test transaction, so {@code @Sql} commits its fixture before the method and a second
 *       {@code AFTER_TEST_METHOD} script cleans up after - the declarative form of an {@code @AfterEach} teardown,
 *       the pattern {@code OrderServiceIT} / {@code SalesIngestServiceIT} do by hand.</li>
 * </ul>
 */
class PersistenceRollbackIT extends AbstractOracleIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@AfterEach
	void safetyNet() {
		jdbcClient.sql("delete from category where name = 'RB-Probe'").update();
		jdbcClient.sql("delete from sales_ledger where ledger_ref like 'SQLPROBE-%'").update();
	}

	private long categoryCount(final String name) {
		return jdbcClient.sql("select count(*) from category where name = ?").param(name).query(Long.class).single();
	}

	private long ledgerCount(final String refLike) {
		return jdbcClient.sql("select count(*) from sales_ledger where ledger_ref like ?")
						 .param(refLike)
						 .query(Long.class)
						 .single();
	}

	@Test
	@Transactional
	void testManagedTransactionRollsBackSoTheRowNeverLands() {
		assertThat(TestTransaction.isActive()).isTrue();

		jdbcClient.sql("insert into category (name, description) values ('RB-Probe', 'transient')").update();
		assertThat(categoryCount("RB-Probe")).isOne(); // visible inside the test transaction

		TestTransaction.flagForRollback(); // already the default for an @Transactional test; explicit for the demo
		TestTransaction.end();

		assertThat(categoryCount("RB-Probe")).isZero(); // the row never committed

		TestTransaction.start(); // leave an active transaction for the framework to close
	}

	@Test
	@Sql(scripts = "/sql/ledger-probe.sql")
	@Sql(scripts = "/sql/ledger-probe-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	void sqlScriptsBracketANonTransactionalTest() {
		// No test transaction here, so the BEFORE script ran and committed in its own transaction.
		assertThat(ledgerCount("SQLPROBE-%")).isOne();
		// The AFTER_TEST_METHOD script deletes it once this method returns.
	}
}
