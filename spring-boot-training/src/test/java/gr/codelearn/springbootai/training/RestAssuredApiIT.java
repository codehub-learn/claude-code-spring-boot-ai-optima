package gr.codelearn.springbootai.training;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * U4.5: the real-HTTP-client counterpart to {@code CatalogApiIT}/{@code OrderApiIT}. Those drive the app through {@code MockMvcTester} — no
 * socket, requests dispatched straight into the servlet container in-process; this hits an embedded Tomcat on a random port through
 * RestAssured's {@code given()/when()/then()} DSL, a real HTTP round trip (headers, status line, JSON body) against real Oracle. Happy path
 * only — every failure path (validation, 404s, content negotiation) is already covered by the {@code MockMvcTester} tests, so duplicating
 * them here through a different HTTP client would test the client, not the app.
 *
 * <p><b>One class, not one per resource.</b> {@code webEnvironment = RANDOM_PORT} is a different Spring TestContext cache key from every
 * other {@code @SpringBootTest} in this module (all default {@code MOCK}), so this is the one place in the suite that pays for a second,
 * real-server context; combining the catalog and order happy paths here means that cost is paid once, not twice.
 *
 * <p>Redeclaring {@code @SpringBootTest} here (rather than only inheriting {@link AbstractOracleIntegrationTest}'s) is the supported way
 * to override just {@code webEnvironment}: Spring resolves the <i>nearest</i> {@code @SpringBootTest} in the class hierarchy, so this
 * annotation wins outright and the parent's {@code @ServiceConnection} Oracle field plus its {@code @ActiveProfiles("test")} are still
 * inherited normally (plain Java field/annotation inheritance, not a Spring TestContext concern).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestAssuredApiIT extends AbstractOracleIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void configureRestAssured() {
		RestAssured.port = port;
	}

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("""
					   delete from order_item
					   where order_id in (select id from "order" where order_number like 'RA-%')
					   """).update();
		jdbcClient.sql("delete from \"order\" where order_number like 'RA-%'").update();
		jdbcClient.sql("delete from product where sku like 'RA-%'").update();
	}

	@Test
	void createThenGetProductRoundTrips() {
		final String body = """
							{"sku":"RA-1","name":"RestAssured SSD","description":"created by RestAssuredApiIT",
							 "price":149.99,"categoryName":"Storage"}""";

		given().contentType(ContentType.JSON).body(body)
			   .when().post("/api/v1/products")
			   .then().statusCode(201).header("Location", "http://localhost:" + port + "/api/v1/products/RA-1");

		// ProductResponse is @JsonNaming(SnakeCaseStrategy.class) (U4.3): categoryName -> category_name on the wire.
		given().when().get("/api/v1/products/{sku}", "RA-1")
			   .then().statusCode(200).body("sku", equalTo("RA-1")).body("category_name", equalTo("Storage"));
	}

	@Test
	void placeThenGetOrderRoundTrips() {
		final String orderNumber = "RA-" + System.nanoTime();
		final String body = """
							{"orderNumber":"%s","customerEmail":"alice@example.com",
							 "shippingAddress":{"line1":"1 RA Way","line2":null,"city":"Testville","postalCode":"00001","country":"US"},
							 "lines":[{"sku":"LAP-0001","quantity":1}]}
							""".formatted(orderNumber);

		given().contentType(ContentType.JSON).body(body)
			   .when().post("/api/v1/orders")
			   .then().statusCode(201);

		// GET-by-number answers the Views.Detail shape (U4.3): the placed line, nested.
		given().when().get("/api/v1/orders/{orderNumber}", orderNumber)
			   .then().statusCode(200)
			   .body("status", equalTo("NEW"))
			   .body("lines", hasSize(1))
			   .body("lines[0].sku", equalTo("LAP-0001"));
	}
}