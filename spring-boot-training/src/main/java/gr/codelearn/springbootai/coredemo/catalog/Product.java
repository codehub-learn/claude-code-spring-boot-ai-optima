package gr.codelearn.springbootai.coredemo.catalog;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record Product(long id, String sku, String name, @Nullable String description, BigDecimal price, int stockQuantity, boolean active,
					long categoryId) {

	public Product {
		if (price.signum() < 0) {
			throw new IllegalArgumentException("Price must not be negative");
		}
		if (stockQuantity < 0) {
			throw new IllegalArgumentException("Stock must not be negative");
		}
	}

	public Product reserve(int quantity) {
		requirePositive(quantity);
		int available = active ? stockQuantity : 0;
		if (quantity > available) {
			throw new InsufficientStockException(id, quantity, available);
		}
		return withStock(stockQuantity - quantity);
	}

	public Product release(int quantity) {
		requirePositive(quantity);
		return withStock(stockQuantity + quantity);
	}

	private Product withStock(int stock) {
		return new Product(id, sku, name, description, price, stock, active, categoryId);
	}

	private static void requirePositive(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be positive");
		}
	}
}
