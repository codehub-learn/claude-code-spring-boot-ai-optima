package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Speed of a {@link Shipment}. {@code SAME_DAY} is only offered inside one depot area.
 */
enum ServiceLevel {
	STANDARD,
	EXPRESS,
	SAME_DAY
}