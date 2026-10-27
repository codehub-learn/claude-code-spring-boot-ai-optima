package gr.codelearn.springbootai.persistencedemo.invoice;

/**
 * Lifecycle of an {@link Invoice}: issued, then paid or past due.
 */
public enum InvoiceStatus {
	ISSUED,
	PAID,
	OVERDUE
}