package gr.codelearn.springbootai.coredemo.catalog;

import gr.codelearn.springbootai.coredemo.common.InMemoryStore;
import org.springframework.stereotype.Repository;

@Repository
class CategoryRepository extends InMemoryStore<Category> {

	CategoryRepository() {
		super(Category::id);
	}
}
