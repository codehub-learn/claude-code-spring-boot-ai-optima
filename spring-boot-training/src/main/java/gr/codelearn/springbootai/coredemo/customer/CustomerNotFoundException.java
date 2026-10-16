package gr.codelearn.springbootai.coredemo.customer;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(long id) {
		super("Customer %d not found".formatted(id));
	}
}
