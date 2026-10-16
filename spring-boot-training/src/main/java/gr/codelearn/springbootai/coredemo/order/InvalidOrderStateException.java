package gr.codelearn.springbootai.coredemo.order;

public class InvalidOrderStateException extends RuntimeException {

	public InvalidOrderStateException(long orderId, OrderStatus from, OrderStatus to) {
		super("Order %d cannot move from %s to %s".formatted(orderId, from, to));
	}
}
