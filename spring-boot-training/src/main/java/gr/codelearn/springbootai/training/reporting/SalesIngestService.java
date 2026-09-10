package gr.codelearn.springbootai.training.reporting;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SimplePropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Bulk-loads {@link SalesLedgerEntry} rows into {@code sales_ledger} - the batch-insert exhibit for U3.5.
 *
 * <p><b>Why not {@code JdbcClient}.</b> {@code JdbcClient} has no batch API in Spring Framework 7.x
 * ({@code params(List)} is positional parameters for one statement; {@code update()} returns one row count), so a real batch drops to
 * {@link NamedParameterJdbcTemplate#batchUpdate(String, SqlParameterSource[])} - one prepared statement, one round trip, {@code addBatch()}
 * per row. {@code CategorySalesQuery} keeps {@code JdbcClient} for the single-statement reads.
 *
 * <p><b>Why {@link SimplePropertySqlParameterSource} and not {@code SqlParameterSourceUtils.createBatch}.</b> The
 * latter wraps each element in a {@code BeanPropertySqlParameterSource}, which reads JavaBean getters and does not see record accessors -
 * {@code SalesLedgerEntry} is a record, so it would bind nothing. {@code SimplePropertySqlParameterSource} resolves {@code :ledgerRef} etc.
 * through the record component.
 *
 * <p>No generated keys are read back (the caller supplies {@code ledgerRef} as the key), which also sidesteps
 * Oracle's "identity column + JDBC batch" limitation. {@code @Transactional} makes the whole import atomic; the plain-JDBC write enlists in
 * the same {@code JpaTransactionManager} the {@code catalog} slice uses.
 */
@Service
class SalesIngestService {

	private final NamedParameterJdbcTemplate jdbc;

	SalesIngestService(final NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Inserts every entry in one batch. Returns the total number of rows written.
	 */
	@Transactional
	int ingest(final List<SalesLedgerEntry> entries) {
		if (entries.isEmpty()) {
			return 0;
		}

		final SqlParameterSource[] batch = entries.stream()
												  .map(SimplePropertySqlParameterSource::new)
												  .toArray(SqlParameterSource[]::new);

		final int[] rowCounts = jdbc.batchUpdate("""
												 insert into sales_ledger (ledger_ref, product_id, sold_on, quantity, net_amount)
												 values (:ledgerRef, :productId, :soldOn, :quantity, :netAmount)
												 """, batch);

		return Arrays.stream(rowCounts).sum();
	}
}