package gr.codelearn.springbootai.coredemo.catalog;

public class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(long id) {
		super("Category %d not found".formatted(id));
	}
}
