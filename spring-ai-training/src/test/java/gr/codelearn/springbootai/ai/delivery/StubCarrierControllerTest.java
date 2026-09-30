package gr.codelearn.springbootai.ai.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 11.3: the stub carrier's scripted postcodes, through the real MVC setup (API versioning included: a first version under
 * {@code /stub-carrier} passed in a standalone MockMvc and was rejected by the running application), which the demo and the README's table
 * rely on. The 10 s "too slow" postcode is left out: sleeping in a unit test proves nothing the HTTP client's timeout test does not.
 */
@WebMvcTest(controllers = StubCarrierController.class, properties = "app.delivery.carrier.stub.enabled=true")
class StubCarrierControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void anyOtherPostcodeGetsAQuote() {
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=10431")).hasStatusOk()
																			   .bodyJson()
																			   .isLenientlyEqualTo(
																					   "{\"days\": 3, \"cost\": 4.90, \"carrier\": \"Stub" +
																					   " " +
																					   "Express\"}");
	}

	@Test
	void theDownPostcodeAlwaysFails() {
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=50000")).hasStatus(503);
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=50000")).hasStatus(503);
	}

	@Test
	void theFlakyPostcodeFailsOnceThenWorks() {
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=50001")).hasStatus(503);
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=50001")).hasStatusOk();
	}

	@Test
	void theBusyPostcodeIsRateLimitedWithARetryAfter() {
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=50002")).hasStatus(429).hasHeader("Retry-After", "30");
	}

	@Test
	void theUnservedPostcodeIsNotFound() {
		assertThat(mvc.get().uri("/api/v1/stub-carrier/quotes?postcode=40404")).hasStatus(404);
	}
}