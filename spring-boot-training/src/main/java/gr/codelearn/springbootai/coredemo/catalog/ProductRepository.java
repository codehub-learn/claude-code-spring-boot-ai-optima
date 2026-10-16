package gr.codelearn.springbootai.coredemo.catalog;

import gr.codelearn.springbootai.coredemo.common.InMemoryStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class ProductRepository extends InMemoryStore<Product> {

	ProductRepository() {
		super(Product::id);
	}

	public Optional<Product> findById(long id) {
		return null;
	}
}
