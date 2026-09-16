package gr.codelearn.springbootai.boot.order;

import gr.codelearn.springbootai.boot.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U4.1/U4.2 HTTP integration test for the order endpoints against real Oracle + the {@code R__010}/{@code R__020} baseline. Not
 * {@code @Transactional} — {@code OrderService.placeOrder} commits and the {@code AFTER_COMMIT} listener fires; {@code API-}-prefixed order
 * numbers are cleaned up in {@link #cleanUp()} (the {@code OrderTransactionSemanticsIT} precedent).
 */
@AutoConfigureMockMvc
class OrderApiIT extends AbstractOracleIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JdbcClient jdbcClient;

	private final String orderNumber = "API-" + System.nanoTime();

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("""
					   delete from order_item
					   where order_id in (select id from "order" where order_number like 'API-%')
					   """).update();
		jdbcClient.sql("delete from \"order\" where order_number like 'API-%'").update();
	}

	private String placeBody() {
		return """
			   {"orderNumber":"%s","customerEmail":"alice@example.com",
			    "shippingAddress":{"line1":"1 API Way","line2":null,"city":"Testville","postalCode":"00001","country":"US"},
			    "lines":[{"sku":"LAP-0001","quantity":1}]}
			   """.formatted(orderNumber);
	}

	@Test
	void placeReadPatchLifecycle() {
		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(placeBody()))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/v1/orders/" + orderNumber);
		// POST answers the Views.Summary shape (U4.3): "totalAmount", not the nested detail fields.
		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
					  .content(placeBody().replace(orderNumber, orderNumber + "-B")))
				.bodyJson().hasPath("$.totalAmount").doesNotHavePath("$.lines");

		assertThat(mvc.get().uri("/api/v1/orders/{n}", orderNumber))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("NEW");
		// GET-by-number answers the Views.Detail shape: the line placed above, nested, plus the shipping address.
		assertThat(mvc.get().uri("/api/v1/orders/{n}", orderNumber))
				.bodyJson().extractingPath("$.lines[0].sku").isEqualTo("LAP-0001");
		assertThat(mvc.get().uri("/api/v1/orders/{n}", orderNumber))
				.bodyJson().extractingPath("$.shippingAddress.city").isEqualTo("Testville");

		assertThat(mvc.patch().uri("/api/v1/orders/{n}/status", orderNumber)
					  .contentType(MediaType.APPLICATION_JSON)
					  .content("""
							   {"status":"PAID"}"""))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("PAID");
	}

	@Test
	void getUnknownOrderReturns404() {
		assertThat(mvc.get().uri("/api/v1/orders/{n}", "API-DOES-NOT-EXIST")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void invalidPlaceBodyReturnsAProblemDetailWithFieldErrors() {
		final String body = """
							{"orderNumber":"%s","customerEmail":"not-an-email",
							 "shippingAddress":{"line1":"1 Way","line2":null,"city":"Town","postalCode":"00001","country":"US"},
							 "lines":[{"sku":"LAP-0001","quantity":0}]}
							""".formatted(orderNumber);

		assertThat(mvc.post().uri("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().extractingPath("$.errors[*].field").asArray().contains("customerEmail", "lines[0].quantity");
	}
}