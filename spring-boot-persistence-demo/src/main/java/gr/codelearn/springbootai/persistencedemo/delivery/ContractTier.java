package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Contract level of a {@link Customer}; higher tiers get a larger discount on shipment prices.
 */
enum ContractTier {
	STANDARD,
	SILVER,
	GOLD
}