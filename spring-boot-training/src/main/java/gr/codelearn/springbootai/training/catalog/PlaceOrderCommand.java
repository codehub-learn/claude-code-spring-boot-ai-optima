package gr.codelearn.springbootai.training.catalog;

import java.util.List;

/**
 * The input to {@link OrderService#placeOrder(PlaceOrderCommand)} — a <b>command object</b> (CLAUDE.md: "records for command/query
 * objects"). It carries only what the caller knows: the new order number, which existing {@link Customer} the order is for (by email), the
 * shipping {@link Address}, and the lines as {@code (sku, quantity)} pairs. {@link OrderFactory} turns it into a persistent {@link Order}
 * aggregate, resolving the customer and products against their repositories.
 *
 * <p>Bean Validation on the fields comes in U4.2, when this type is also reached from a controller; for now the factory validates by
 * throwing {@link CatalogNotFoundException} when a referenced row is missing.
 */
record PlaceOrderCommand(String orderNumber, String customerEmail, Address shippingAddress, List<Line> lines) {

	/**
	 * One requested line: a product {@code sku} and how many. The unit price is not the caller's to set — {@link OrderFactory} reads it
	 * from the current {@link Product#getPrice()} so the order captures the price at order time.
	 */
	record Line(String sku, int quantity) {
	}
}
