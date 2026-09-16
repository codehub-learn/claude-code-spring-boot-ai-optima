package gr.codelearn.springbootai.boot.order.internal;

import gr.codelearn.springbootai.boot.order.OrderService;

/**
 * The order was placed but the receipt could not be delivered. A <b>checked</b> exception on purpose: Spring's default rollback rule does
 * <i>not</i> roll back for checked exceptions, so {@link OrderService#placeOrderAndDeliverReceipt(PlaceOrderCommand)} has to opt in with
 * {@code @Transactional(rollbackFor = ReceiptDeliveryException.class)}. Without that attribute the order would commit even though this was
 * thrown.
 */
public class ReceiptDeliveryException extends Exception {

	public ReceiptDeliveryException(final String message) {
		super(message);
	}
}