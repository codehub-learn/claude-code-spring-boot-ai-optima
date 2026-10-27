package gr.codelearn.springbootai.persistencedemo.shipment;

/**
 * Speed of a {@link Shipment}. {@code SAME_DAY} is only offered inside one depot area.
 */
public enum ServiceLevel {
	STANDARD,
	EXPRESS,
	SAME_DAY
}