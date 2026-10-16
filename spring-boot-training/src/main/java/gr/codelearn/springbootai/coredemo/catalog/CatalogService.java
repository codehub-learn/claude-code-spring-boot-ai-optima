package gr.codelearn.springbootai.coredemo.catalog;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogService {

	private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

	private final CategoryRepository categories;
	private final ProductRepository products;

	CatalogService(CategoryRepository categories, ProductRepository products) {
		this.categories = categories;
		this.products = products;
	}

	public Category createCategory(String name, @Nullable String description, @Nullable Long parentId) {
		if (parentId != null) {
			findCategory(parentId);
		}
		return categories.save(new Category(categories.nextId(), name, description, parentId));
	}

	public Category findCategory(long id) {
		return categories.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

	public Product createProduct(String sku, String name, @Nullable String description, BigDecimal price, int stockQuantity,
								 long categoryId) {
		findCategory(categoryId);
		var product = products.save(new Product(products.nextId(), sku, name, description, price, stockQuantity, true, categoryId));
		log.info("Created product {} ({})", product.id(), product.sku());
		return product;
	}

	public Product findProduct(long id) {
		return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	/**
	 * Active products only.
	 */
	public List<Product> listProducts() {
		return products.findAll().stream().filter(Product::active).toList();
	}

	/**
	 * Atomically takes {@code quantity} units out of stock and returns the updated product.
	 */
	public Product reserveStock(long productId, int quantity) {
		return products.update(productId, product -> product.reserve(quantity)).orElseThrow(() -> new ProductNotFoundException(productId));
	}

	public Product releaseStock(long productId, int quantity) {
		return products.update(productId, product -> product.release(quantity)).orElseThrow(() -> new ProductNotFoundException(productId));
	}
}
