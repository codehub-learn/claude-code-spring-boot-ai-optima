package gr.codelearn.springbootai.training.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row to append to the {@code sales_ledger} fact table - the input to {@link SalesIngestService#ingest}. A plain carrier for a bulk
 * import (a nightly ETL, a CSV upload): {@code ledgerRef} is the caller-supplied natural key that makes a re-import idempotent,
 * {@code productId} a soft reference into {@code product}.
 *
 * @param ledgerRef unique per row across all imports (e.g. {@code "IMP-2026-02-000042"})
 * @param productId the {@code product.id} this sale is for (not FK-enforced in the reporting store)
 * @param soldOn    the calendar date of the sale
 * @param quantity  units sold, must be positive
 * @param netAmount net revenue for the row
 */
record SalesLedgerEntry(String ledgerRef, long productId, LocalDate soldOn, int quantity, BigDecimal netAmount) {
}