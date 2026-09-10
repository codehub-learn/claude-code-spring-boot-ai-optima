package gr.codelearn.springbootai.training.catalog;

/**
 * One requested order line on {@link PlaceOrderRequest}: a product {@code sku} and a quantity. The unit price is not the client's to set —
 * {@link OrderFactory} captures it from the current {@link Product#getPrice()} at order time.
 */
record OrderLineRequest(String sku, int quantity) {
}
