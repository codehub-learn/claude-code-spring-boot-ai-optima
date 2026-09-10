package gr.codelearn.springbootai.training.catalog;

/**
 * A line was ordered in a quantity the warehouse cannot fully cover yet — a business <i>warning</i>, not a failure: the order should still
 * be placed and back-ordered. It is a {@code RuntimeException} (which the default rule <i>would</i> roll back), so
 * {@link OrderService#placeOrderToleratingLowStock(PlaceOrderCommand)} overrides that with
 * {@code @Transactional(noRollbackFor = LowStockException.class)} — the order commits and the exception still propagates to the caller.
 */
class LowStockException extends RuntimeException {

	LowStockException(final String message) {
		super(message);
	}
}
