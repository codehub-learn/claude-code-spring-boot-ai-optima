package gr.codelearn.springbootai.coredemo.catalog;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(long id) {
		super("Product %d not found".formatted(id));
	}
}
