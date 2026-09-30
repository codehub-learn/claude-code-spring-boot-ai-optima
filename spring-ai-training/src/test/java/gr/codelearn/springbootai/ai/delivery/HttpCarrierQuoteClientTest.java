package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ExternalServiceException.Kind;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit 11.3: how the carrier client classifies each way the call can go, and which ones it asks again. The HTTP layer is mocked
 * ({@link MockRestServiceServer}), so each answer is scripted; the real timeout is proven once against a JDK HTTP server.
 */
class HttpCarrierQuoteClientTest {

	private static final String QUOTE_URL = "http://carrier.test/quotes?postcode=10431";
	private static final String QUOTE = "{\"days\":3,\"cost\":4.90,\"carrier\":\"Stub Express\"}";

	private final RestClient.Builder builder = RestClient.builder().baseUrl("http://carrier.test");
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final CarrierQuoteClient client = new HttpCarrierQuoteClient(builder.build(), properties(2, Duration.ofMillis(1)));

	@Test
	void aQuoteIsReadFromTheCarriersAnswer() {
		server.expect(once(), requestTo(QUOTE_URL))
			  .andExpect(method(HttpMethod.GET))
			  .andRespond(withSuccess(QUOTE, MediaType.APPLICATION_JSON));

		final CarrierQuoteClient.Quote quote = client.quote("10431");

		assertThat(quote.days()).isEqualTo(3);
		assertThat(quote.cost()).isEqualByComparingTo("4.90");
		assertThat(quote.carrier()).isEqualTo("Stub Express");
		server.verify();
	}

	@Test
	void aFailureThenAnAnswerIsARetryThatWorked() {
		server.expect(once(), requestTo(QUOTE_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		server.expect(once(), requestTo(QUOTE_URL)).andRespond(withSuccess(QUOTE, MediaType.APPLICATION_JSON));

		assertThat(client.quote("10431").days()).isEqualTo(3);
		server.verify();
	}

	@Test
	void aCarrierThatStaysDownIsAskedThreeTimesThenReportedUnavailable() {
		server.expect(times(3), requestTo(QUOTE_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("internal stack trace"));

		assertThatThrownBy(() -> client.quote("10431")).isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
			assertThat(ex.kind()).isEqualTo(Kind.UNAVAILABLE);
			assertThat(ex.getMessage()).isEqualTo("The delivery service is not responding (3 attempts)")
									   .doesNotContain("503")
									   .doesNotContain("stack trace")
									   .doesNotContain("carrier.test");
		});
		server.verify();
	}

	@Test
	void aConnectionFailureCountsAsUnavailableToo() {
		server.expect(times(3), requestTo(QUOTE_URL))
			  .andRespond(withException(new IOException("Connection refused: carrier.internal:8443")));

		assertThatThrownBy(() -> client.quote("10431")).isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
			assertThat(ex.kind()).isEqualTo(Kind.UNAVAILABLE);
			assertThat(ex.getMessage()).doesNotContain("carrier.internal");
			// The detail is kept for the log, on the cause.
			assertThat(ex.getCause()).hasMessageContaining("carrier.internal");
		});
		server.verify();
	}

	@Test
	void aRateLimitIsNotRetried() {
		server.expect(once(), requestTo(QUOTE_URL))
			  .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "30"));

		assertThatThrownBy(() -> client.quote("10431")).isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
			assertThat(ex.kind()).isEqualTo(Kind.RATE_LIMITED);
			assertThat(ex.getMessage()).isEqualTo("The delivery service is busy (too many requests)");
		});
		server.verify();
	}

	@Test
	void aRefusedPostcodeIsNotRetriedAndNamesThePostcode() {
		server.expect(once(), requestTo("http://carrier.test/quotes?postcode=40404")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> client.quote("40404")).isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
			assertThat(ex.kind()).isEqualTo(Kind.REJECTED);
			assertThat(ex.getMessage()).isEqualTo("The carrier does not deliver to postcode 40404");
		});
		server.verify();
	}

	@Test
	void anAnswerThatIsNotAQuoteIsNotRetriedEither() {
		server.expect(once(), requestTo(QUOTE_URL)).andRespond(withSuccess("<html>maintenance</html>", MediaType.TEXT_HTML));

		assertThatThrownBy(() -> client.quote("10431")).isInstanceOfSatisfying(ExternalServiceException.class,
																			   ex -> assertThat(ex.kind()).isEqualTo(Kind.REJECTED));
		server.verify();
	}

	@Test
	void withoutRetriesTheFirstFailureIsTheLastAndTheMessageSaysOneAttempt() {
		final var single = RestClient.builder().baseUrl("http://carrier.test");
		final var singleServer = MockRestServiceServer.bindTo(single).build();
		singleServer.expect(once(), requestTo(QUOTE_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThatThrownBy(() -> new HttpCarrierQuoteClient(single.build(), properties(0, Duration.ofMillis(1))).quote("10431"))
				.hasMessage("The delivery service is not responding (1 attempt)");
		singleServer.verify();
	}

	@Test
	void aCarrierThatHangsIsCutOffByTheReadTimeoutInsteadOfHoldingTheToolCall() throws IOException {
		final HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		slow.createContext("/quotes", exchange -> {
			try {
				Thread.sleep(1_500);
				exchange.sendResponseHeaders(200, -1);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			} finally {
				exchange.close();
			}
		});
		slow.start();
		try {
			final var properties = new DeliveryProperties(URI.create("http://127.0.0.1:" + slow.getAddress().getPort()),
														  Duration.ofSeconds(1), Duration.ofMillis(150), 1, Duration.ofMillis(10));
			final CarrierQuoteClient timeoutClient = new DeliveryConfig().carrierQuoteClient(RestClient.builder(), properties);

			final long start = System.nanoTime();
			assertThatThrownBy(() -> timeoutClient.quote("10431")).isInstanceOfSatisfying(ExternalServiceException.class, ex -> {
				assertThat(ex.kind()).isEqualTo(Kind.UNAVAILABLE);
				assertThat(ex.getMessage()).isEqualTo("The delivery service is not responding (2 attempts)");
				assertThat(ex.getCause()).hasRootCauseInstanceOf(HttpTimeoutException.class);
			});
			assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1_400));
		} finally {
			slow.stop(0);
		}
	}

	private static DeliveryProperties properties(final int maxRetries, final Duration retryDelay) {
		return new DeliveryProperties(URI.create("http://carrier.test"), Duration.ofSeconds(1), Duration.ofSeconds(1), maxRetries,
									  retryDelay);
	}
}
