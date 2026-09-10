package gr.codelearn.springbootai.training.catalog;

/**
 * The order was placed but the receipt could not be delivered. A <b>checked</b> exception on purpose: Spring's default rollback rule does
 * <i>not</i> roll back for checked exceptions, so {@link OrderService#placeOrderAndDeliverReceipt(PlaceOrderCommand)} has to opt in with
 * {@code @Transactional(rollbackFor = ReceiptDeliveryException.class)}. Without that attribute the order would commit even though this was
 * thrown.
 */
class ReceiptDeliveryException extends Exception {

	ReceiptDeliveryException(final String message) {
		super(message);
	}
}
