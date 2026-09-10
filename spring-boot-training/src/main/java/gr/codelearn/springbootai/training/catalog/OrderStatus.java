package gr.codelearn.springbootai.training.catalog;

/**
 * Lifecycle of an {@link Order}. Persisted as its {@code name()} via {@code @Enumerated(EnumType.STRING)} — readable in the database and
 * safe to reorder, unlike {@code ORDINAL}. The {@code ck_order_status} check constraint in {@code V2__create_orders.sql} pins the column to
 * exactly these names.
 */
enum OrderStatus {
	NEW,
	PAID,
	SHIPPED,
	DELIVERED,
	CANCELLED
}
