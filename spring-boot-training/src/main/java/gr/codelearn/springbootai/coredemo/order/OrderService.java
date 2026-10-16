package gr.codelearn.springbootai.coredemo.order;

import gr.codelearn.springbootai.coredemo.catalog.CatalogService;
import gr.codelearn.springbootai.coredemo.catalog.Product;
import gr.codelearn.springbootai.coredemo.customer.Address;
import gr.codelearn.springbootai.coredemo.customer.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderRepository orders;
	private final CustomerService customerService;
	private final CatalogService catalogService;
	private final Clock clock;

	OrderService(OrderRepository orders, CustomerService customerService, CatalogService catalogService, Clock clock) {
		this.orders = orders;
		this.customerService = customerService;
		this.catalogService = catalogService;
		this.clock = clock;
	}

	/** Reserves stock for every line; if any line fails, the lines already reserved are released and the failure is rethrown. */
	public Order placeOrder(long customerId, Address shippingAddress, List<OrderLineRequest> lines) {
		if (lines.isEmpty()) {
			throw new IllegalArgumentException("An order needs at least one line");
		}
		customerService.findById(customerId);

		var items = new ArrayList<OrderItem>();
		try {
			for (var line : lines) {
				Product product = catalogService.reserveStock(line.productId(), line.quantity());
				items.add(new OrderItem(product.id(), product.name(), line.quantity(), product.price()));
			}
		} catch (RuntimeException ex) {
			releaseStock(items);
			throw ex;
		}

		var order = orders.save(Order.place(orders.nextId(), customerId, shippingAddress, items, clock.instant()));
		log.info("Placed order {} for customer {}", order.orderNumber(), customerId);
		return order;
	}

	public Order findById(long id) {
		return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}

	/** Cancels the order and puts its stock back. */
	public Order cancelOrder(long id) {
		var cancelled = transition(id, OrderStatus.CANCELLED);
		releaseStock(cancelled.items());
		log.info("Cancelled order {}", cancelled.orderNumber());
		return cancelled;
	}

	/** Called by the payment feature once a payment succeeds. */
	public Order markPaid(long id) {
		return transition(id, OrderStatus.PAID);
	}

	private Order transition(long id, OrderStatus next) {
		return orders.update(id, order -> order.withStatus(next)).orElseThrow(() -> new OrderNotFoundException(id));
	}

	private void releaseStock(List<OrderItem> items) {
		items.forEach(item -> catalogService.releaseStock(item.productId(), item.quantity()));
	}
}
