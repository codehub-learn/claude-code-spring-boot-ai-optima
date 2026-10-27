package gr.codelearn.springbootai.persistencedemo.customer;

/**
 * Contract level of a {@link Customer}; higher tiers get a larger discount on shipment prices.
 */
public enum ContractTier {
	STANDARD,
	SILVER,
	GOLD
}