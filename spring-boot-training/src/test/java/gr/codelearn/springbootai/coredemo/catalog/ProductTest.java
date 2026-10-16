package gr.codelearn.springbootai.coredemo.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

	private static Product product(int stock, boolean active) {
		return new Product(1, "SKU-1", "Mug", null, new BigDecimal("9.90"), stock, active, 1);
	}

	@Test
	void reserveReducesStock() {
		assertThat(product(5, true).reserve(2).stockQuantity()).isEqualTo(3);
	}

	@Test
	void reserveBeyondStockFails() {
		assertThatThrownBy(() -> product(1, true).reserve(2)).isInstanceOf(InsufficientStockException.class);
	}

	@Test
	void inactiveProductCannotBeReserved() {
		assertThatThrownBy(() -> product(5, false).reserve(1)).isInstanceOf(InsufficientStockException.class);
	}

	@Test
	void releaseRestoresStock() {
		assertThat(product(3, true).release(2).stockQuantity()).isEqualTo(5);
	}

	@Test
	void negativeStockIsRejected() {
		assertThatThrownBy(() -> product(-1, true)).isInstanceOf(IllegalArgumentException.class);
	}
}
