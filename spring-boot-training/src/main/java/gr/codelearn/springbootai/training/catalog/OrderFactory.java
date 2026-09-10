package gr.codelearn.springbootai.training.catalog;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Turns a {@link PlaceOrderCommand} into a transient {@link Order} aggregate: looks up the {@link Customer} by email and each
 * {@link Product} by sku, then builds the lines through {@link Order#addItem}. The unit price is copied from the product row so the order
 * records the price <i>at order time</i>.
 *
 * <p>No {@code @Transactional} here on purpose. This is a plain collaborator that always runs inside the caller's transaction
 * ({@link OrderService} or {@link OrderPlacementService}); giving it its own transaction attribute would either start a nested one or
 * quietly do nothing, and it needs neither — the repository reads join whatever transaction is already open.
 *
 * <p>A missing customer or product is a {@link CatalogNotFoundException} (unchecked) — the caller's transaction then rolls back under the
 * default rule.
 */
@Component
class OrderFactory {

	private final CustomerRepository customerRepository;
	private final ProductRepository productRepository;

	OrderFactory(final CustomerRepository customerRepository, final ProductRepository productRepository) {
		this.customerRepository = customerRepository;
		this.productRepository = productRepository;
	}

	Order build(final PlaceOrderCommand command) {
		final Customer customer = customerRepository.findByEmail(command.customerEmail())
													.orElseThrow(() -> new CatalogNotFoundException(
															"No customer with email " + command.customerEmail()));

		final Order order = new Order(command.orderNumber(), customer, OffsetDateTime.now(), command.shippingAddress());
		for (final PlaceOrderCommand.Line line : command.lines()) {
			final Product product = productRepository.findBySku(line.sku())
													 .orElseThrow(() -> new CatalogNotFoundException(
															 "No product with sku " + line.sku()));
			order.addItem(product, line.quantity(), product.getPrice());
		}
		return order;
	}
}