package gr.codelearn.springbootai.training.order;

import gr.codelearn.springbootai.training.common.NotFoundException;
import gr.codelearn.springbootai.training.order.internal.OrderConfirmation;
import gr.codelearn.springbootai.training.order.mapper.OrderWebMapper;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * U4.1/U4.2 slice test for {@link OrderController}. The real {@link OrderWebMapper} is imported (it is a plain record→record converter);
 * {@link OrderService} is mocked. Covers {@code POST → 201 + Location}, read-back, the {@code PATCH} status transition, the {@code 404}
 * failure path, a malformed body → {@code 400}, and (U4.2) a {@code @Valid} nested-body failure and an invalid {@code @Email} query param.
 */
@WebMvcTest(controllers = OrderController.class,
			excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebMvcConfigurer.class))
@Import(OrderWebMapper.class)
class OrderControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OrderService orderService;

	private static OrderConfirmation confirmation(final OrderStatus status) {
		return new OrderConfirmation("API-1", status, new BigDecimal("1299.00"), 1);
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
	void getOrderReturns200() {
		given(orderService.findOrder("API-1")).willReturn(confirmation(OrderStatus.NEW));

		assertThat(mvc.get().uri("/api/v1/orders/{n}", "API-1"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("NEW");
	}

	@Test
	void getUnknownOrderReturns404() {
		given(orderService.findOrder("NOPE")).willThrow(new NotFoundException("No order NOPE"));

		assertThat(mvc.get().uri("/api/v1/orders/{n}", "NOPE")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void patchStatusReturns200WithTheNewStatus() {
		given(orderService.updateStatus("API-1", OrderStatus.PAID)).willReturn(confirmation(OrderStatus.PAID));

		assertThat(mvc.patch().uri("/api/v1/orders/{n}/status", "API-1")
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"status":"PAID"}"""))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("PAID");
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