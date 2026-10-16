package gr.codelearn.springbootai.coredemo.payment;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(long orderId) {
		super("No payment found for order %d".formatted(orderId));
	}
}
