package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
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
 * U4.1 HTTP integration test for the order endpoints against real Oracle + the {@code R__010}/{@code R__020} baseline. Not
 * {@code @Transactional} — {@code OrderService.placeOrder} commits and the {@code AFTER_COMMIT} listener fires; {@code API-}-prefixed order
 * numbers are cleaned up in {@link #cleanUp()} (the {@code OrderServiceIT} precedent).
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

		assertThat(mvc.get().uri("/api/v1/orders/{n}", orderNumber))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("NEW");

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
}