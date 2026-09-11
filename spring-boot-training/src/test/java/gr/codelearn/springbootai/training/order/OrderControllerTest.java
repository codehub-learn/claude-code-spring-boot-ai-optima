package gr.codelearn.springbootai.training.order;

import gr.codelearn.springbootai.training.common.NotFoundException;
import gr.codelearn.springbootai.training.order.dto.AddressResponse;
import gr.codelearn.springbootai.training.order.dto.OrderDetailResponse;
import gr.codelearn.springbootai.training.order.dto.OrderLineResponse;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.mapper.OrderWebMapperImpl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * U4.1/U4.2/U4.3/U4.4 slice test for {@link OrderController}. The real, MapStruct-generated {@code OrderWebMapperImpl} is imported directly
 * by its generated class name - a {@code @WebMvcTest} slice does not component-scan {@code order.mapper}, and {@code @Import} needs a
 * concrete class, not the {@code OrderWebMapper} interface it implements; {@link OrderService} is mocked. Covers
 * {@code POST → 201 + Location}, read-back (both the {@code Views.Summary} shape common to place/list/patch-status and the
 * {@code Views.Detail} shape unique to get-one), the {@code PATCH} status transition, the {@code 404} failure path, a malformed body →
 * {@code 400}, and (U4.2) a {@code @Valid} nested-body failure and an invalid {@code @Email} query param.
 */
@WebMvcTest(controllers = OrderController.class,
			excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfigurer.class))
@Import(OrderWebMapperImpl.class)
class OrderControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OrderService orderService;

	private static OrderConfirmation confirmation(final OrderStatus status) {
		return new OrderConfirmation("API-1", status, new BigDecimal("1299.00"), 1);
	}

	private static OrderDetailResponse detail(final OrderStatus status) {
		final AddressResponse address = new AddressResponse("1 Way", null, "Town", "00001", "US");
		final List<OrderLineResponse> lines = List.of(
				new OrderLineResponse("LAP-0001", "Latitude 14 Ultralight", 1, new BigDecimal("1299.00"), new BigDecimal("1299.00")));
		return new OrderDetailResponse(
				"API-1", status, new BigDecimal("1299.00"), 1, "alice@example.com", OffsetDateTime.now(), address, lines);
	}

	private static final String PLACE_BODY = """
											 {"orderNumber":"API-1","customerEmail":"alice@example.com",
											  "shippingAddress":{"line1":"1 Way","line2":null,"city":"Town","postalCode":"00001","country":"US"},
											  "lines":[{"sku":"LAP-0001","quantity":1}]}
											 """;

	@Test
	void placeOrderReturns201WithLocation() {
		given(orderService.placeOrder(any())).willReturn(confirmation(OrderStatus.NEW));

		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(PLACE_BODY))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/orders/API-1");
	}

	@Test
	void placeOrderResponseIsTheSummaryViewOnly() {
		// @JsonView(Views.Summary.class): total serializes as "totalAmount" (@JsonProperty), and the Views.Detail-only fields
		// (lines, shippingAddress, customerEmail, placedAt) never appear, even though OrderWebMapper.toResponse leaves them
		// null/empty rather than omitting them from the record.
		given(orderService.placeOrder(any())).willReturn(confirmation(OrderStatus.NEW));

		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(PLACE_BODY))
				.bodyJson()
				.hasPath("$.totalAmount")
				.doesNotHavePath("$.total")
				.doesNotHavePath("$.lines")
				.doesNotHavePath("$.shippingAddress")
				.doesNotHavePath("$.customerEmail");
	}

	@Test
	void getOrderReturnsTheDetailViewWithNestedLinesAndAddress() {
		given(orderService.getOrderDetail("API-1")).willReturn(detail(OrderStatus.NEW));

		assertThat(mvc.get().uri("/api/v1/orders/{n}", "API-1"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status").isEqualTo("NEW");
		assertThat(mvc.get().uri("/api/v1/orders/{n}", "API-1"))
				.bodyJson().extractingPath("$.lines[0].sku").isEqualTo("LAP-0001");
		assertThat(mvc.get().uri("/api/v1/orders/{n}", "API-1"))
				.bodyJson().extractingPath("$.shippingAddress.city").isEqualTo("Town");
	}

	@Test
	void getUnknownOrderReturns404() {
		given(orderService.getOrderDetail("NOPE")).willThrow(new NotFoundException("No order NOPE"));

		assertThat(mvc.get().uri("/api/v1/orders/{n}", "NOPE")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void patchStatusReturns200WithTheNewStatusAndNoDetailFields() {
		given(orderService.updateStatus("API-1", OrderStatus.PAID)).willReturn(confirmation(OrderStatus.PAID));

		assertThat(mvc.patch().uri("/api/v1/orders/{n}/status", "API-1")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"status":"PAID"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status").isEqualTo("PAID");
		assertThat(mvc.patch().uri("/api/v1/orders/{n}/status", "API-1")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"status":"PAID"}"""))
				.bodyJson().doesNotHavePath("$.lines");
	}

	@Test
	void malformedBodyReturns400() {
		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void emptyLinesReturns400WithFieldErrors() {
		// @NotEmpty on lines + @NotBlank on the nested AddressRequest.line1 — a top-level and a cascaded @Valid failure together.
		assertThat(mvc.post().uri("/api/v1/orders")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							     {"orderNumber":"API-1","customerEmail":"alice@example.com",
							   "shippingAddress":{"line1":"","line2":null,"city":"Town","postalCode":"00001","country":"US"},
							   "lines":[]}
							   """))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.contains("lines", "shippingAddress.line1");
	}

	@Test
	void malformedCustomerEmailFilterReturns400() {
		assertThat(mvc.get().uri("/api/v1/orders").param("customerEmail", "not-an-email")).hasStatus(HttpStatus.BAD_REQUEST);
	}
}