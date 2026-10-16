package gr.codelearn.springbootai.coredemo.order;

import java.math.BigDecimal;

/** A line of an order; name and unit price are snapshots taken when the order is placed. */
public record OrderItem(long productId, String productName, int quantity, BigDecimal unitPrice) {

	public BigDecimal lineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
