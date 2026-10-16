package gr.codelearn.springbootai.coredemo;

import gr.codelearn.springbootai.coredemo.catalog.CatalogService;
import gr.codelearn.springbootai.coredemo.catalog.InsufficientStockException;
import gr.codelearn.springbootai.coredemo.catalog.Product;
import gr.codelearn.springbootai.coredemo.customer.Address;
import gr.codelearn.springbootai.coredemo.customer.AddressType;
import gr.codelearn.springbootai.coredemo.customer.Customer;
import gr.codelearn.springbootai.coredemo.customer.CustomerService;
import gr.codelearn.springbootai.coredemo.order.InvalidOrderStateException;
import gr.codelearn.springbootai.coredemo.order.OrderLineRequest;
import gr.codelearn.springbootai.coredemo.order.OrderService;
import gr.codelearn.springbootai.coredemo.order.OrderStatus;
import gr.codelearn.springbootai.coredemo.payment.PaymentMethod;
import gr.codelearn.springbootai.coredemo.payment.PaymentService;
import gr.codelearn.springbootai.coredemo.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end order flow over the public service APIs. Beans are shared, so every test creates its own customer and products. */
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class EshopFlowTest {

	private static final Address SHIPPING = new Address("1 Main St", "Athens", "10431", "GR", AddressType.SHIPPING);

	private final CustomerService customerService;
	private final CatalogService catalogService;
	private final OrderService orderService;
	private final PaymentService paymentService;

	EshopFlowTest(CustomerService customerService, CatalogService catalogService, OrderService orderService,
				PaymentService paymentService) {
		this.customerService = customerService;
		this.catalogService = catalogService;
		this.orderService = orderService;
		this.paymentService = paymentService;
	}

	private Customer customer() {
		return customerService.register("jane@example.com", "Jane", "Doe", null);
	}

	private Product product(String sku, String price, int stock) {
		var category = catalogService.createCategory("Home", null, null);
		return catalogService.createProduct(sku, sku, null, new BigDecimal(price), stock, category.id());
	}

	@Test
	void placeAndPayOrder() {
		var customer = customer();
		var mug = product("MUG", "25.00", 10);

		var order = orderService.placeOrder(customer.id(), SHIPPING, List.of(new OrderLineRequest(mug.id(), 3)));

		assertThat(order.status()).isEqualTo(OrderStatus.NEW);
		assertThat(order.totalAmount()).isEqualByComparingTo("75.00");
		assertThat(catalogService.findProduct(mug.id()).stockQuantity()).isEqualTo(7);

		var payment = paymentService.pay(order.id(), PaymentMethod.CARD);

		assertThat(payment.status()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(payment.amount()).isEqualByComparingTo("75.00");
		assertThat(orderService.findById(order.id()).status()).isEqualTo(OrderStatus.PAID);
		assertThat(paymentService.findByOrderId(order.id())).isEqualTo(payment);
	}

	@Test
	void failedLineReleasesAlreadyReservedStock() {
		var customer = customer();
		var plenty = product("PLENTY", "5.00", 10);
		var scarce = product("SCARCE", "5.00", 1);

		assertThatThrownBy(() -> orderService.placeOrder(customer.id(), SHIPPING,
				List.of(new OrderLineRequest(plenty.id(), 4), new OrderLineRequest(scarce.id(), 2))))
				.isInstanceOf(InsufficientStockException.class);

		assertThat(catalogService.findProduct(plenty.id()).stockQuantity()).isEqualTo(10);
		assertThat(catalogService.findProduct(scarce.id()).stockQuantity()).isEqualTo(1);
	}

	@Test
	void cancelReleasesStockAndBlocksPayment() {
		var customer = customer();
		var mug = product("CANCEL-MUG", "10.00", 5);
		var order = orderService.placeOrder(customer.id(), SHIPPING, List.of(new OrderLineRequest(mug.id(), 2)));

		var cancelled = orderService.cancelOrder(order.id());

		assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(catalogService.findProduct(mug.id()).stockQuantity()).isEqualTo(5);
		assertThatThrownBy(() -> paymentService.pay(order.id(), PaymentMethod.PAYPAL)).isInstanceOf(InvalidOrderStateException.class);
		assertThatThrownBy(() -> orderService.cancelOrder(order.id())).isInstanceOf(InvalidOrderStateException.class);
	}
}
