package gr.codelearn.springbootai.coredemo.catalog;

public class InsufficientStockException extends RuntimeException {

	public InsufficientStockException(long productId, int requested, int available) {
		super("Product %d: requested %d but only %d available".formatted(productId, requested, available));
	}
}
