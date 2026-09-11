package gr.codelearn.springbootai.training.order.mapper;

import gr.codelearn.springbootai.training.catalog.Category;
import gr.codelearn.springbootai.training.catalog.Product;
import gr.codelearn.springbootai.training.order.Address;
import gr.codelearn.springbootai.training.order.Customer;
import gr.codelearn.springbootai.training.order.Order;
import gr.codelearn.springbootai.training.order.dto.AddressRequest;
import gr.codelearn.springbootai.training.order.dto.OrderDetailResponse;
import gr.codelearn.springbootai.training.order.dto.OrderLineRequest;
import gr.codelearn.springbootai.training.order.dto.OrderLineResponse;
import gr.codelearn.springbootai.training.order.dto.PlaceOrderRequest;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.internal.PlaceOrderCommand;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static gr.codelearn.springbootai.training.order.OrderStatus.NEW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * U4.4: plain unit tests for the MapStruct-generated {@code OrderWebMapperImpl} — no Spring context needed,
 * {@code componentModel = "spring"} still gives the generated class a public no-arg constructor. Covers the nested + collection mapping in
 * {@link OrderWebMapper#toCommand}, the hand-written {@link OrderWebMapper#toResponse(OrderConfirmation)} default method, and the
 * {@code @Mapping(expression = ...)} calculations in {@link OrderWebMapper#toDetailResponse}.
 */
class OrderWebMapperTest {

	private final OrderWebMapper mapper = new OrderWebMapperImpl();

	@Test
	void toCommandMapsNestedAddressAndLineCollection() {
		final PlaceOrderRequest request = new PlaceOrderRequest(
				"ORD-1", "alice@example.com", new AddressRequest("1 Way", null, "Town", "00001", "US"),
				List.of(new OrderLineRequest("LAP-0001", 2)));

		final PlaceOrderCommand command = mapper.toCommand(request);

		assertThat(command.orderNumber()).isEqualTo("ORD-1");
		assertThat(command.shippingAddress()).isEqualTo(new Address("1 Way", null, "Town", "00001", "US"));
		assertThat(command.lines()).containsExactly(new PlaceOrderCommand.Line("LAP-0001", 2));
	}

	@Test
	void toResponseFromConfirmationLeavesDetailFieldsEmpty() {
		final OrderConfirmation confirmation = new OrderConfirmation("ORD-1", NEW, new BigDecimal("1299.00"), 1);

		final OrderDetailResponse response = mapper.toResponse(confirmation);

		assertThat(response.orderNumber()).isEqualTo("ORD-1");
		assertThat(response.customerEmail()).isNull();
		assertThat(response.shippingAddress()).isNull();
		assertThat(response.lines()).isEmpty();
	}

	@Test
	void toDetailResponseMapsTheWholeAggregate() {
		final Customer customer = new Customer("alice@example.com", "Alice", "A.",
											   new Address("2 Home St", null, "Town", "00002", "US"));
		final Address shippingAddress = new Address("1 Way", "Suite 2", "Town", "00001", "US");
		final Order order = new Order("ORD-1", customer, OffsetDateTime.now(), shippingAddress);
		final Category category = new Category("Laptops", null);
		final Product product = new Product("LAP-0001", "Latitude 14", new BigDecimal("1299.00"), category);
		order.addItem(product, 2, new BigDecimal("1299.00"));

		final OrderDetailResponse response = mapper.toDetailResponse(order);

		assertThat(response.customerEmail()).isEqualTo("alice@example.com");
		assertThat(response.lineCount()).isEqualTo(1);
		assertThat(response.shippingAddress().line2()).isEqualTo("Suite 2");
		final OrderLineResponse line = response.lines().getFirst();
		assertThat(line.sku()).isEqualTo("LAP-0001");
		assertThat(line.productName()).isEqualTo("Latitude 14");
		assertThat(line.lineTotal()).isEqualByComparingTo("2598.00");
	}
}