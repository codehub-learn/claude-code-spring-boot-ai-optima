package gr.codelearn.springbootai.coredemo.order;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(long id) {
		super("Order %d not found".formatted(id));
	}
}
