package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Lifecycle of an {@link Invoice}: issued, then paid or past due.
 */
enum InvoiceStatus {
	ISSUED,
	PAID,
	OVERDUE
}