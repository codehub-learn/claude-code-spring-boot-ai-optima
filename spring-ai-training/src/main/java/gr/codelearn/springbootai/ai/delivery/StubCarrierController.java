package gr.codelearn.springbootai.ai.delivery;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit 11.3: a stand-in for the carrier's API, so the failure handling can be seen without a real carrier. Only there when
 * {@code app.delivery.carrier.stub.enabled=true} ({@code application-local.yml}); it answers over real HTTP on the application's own port,
 * so the client, its timeouts and its retries are the real ones. It sits under {@code /api/v1} like every controller: with path-segment API
 * versioning (segment 1) a path such as {@code /stub-carrier/quotes} is read as version "quotes" and rejected. The postcode picks the
 * behavior:
 *
 * <table>
 *   <caption>Postcodes with a scripted behavior</caption>
 *   <tr><th>Postcode</th><th>Answer</th><th>What it shows</th></tr>
 *   <tr><td>{@code 50000}</td><td>{@code 503}</td><td>the carrier is down: retries run out</td></tr>
 *   <tr><td>{@code 50001}</td><td>{@code 503} once, then a quote</td><td>a retry that works</td></tr>
 *   <tr><td>{@code 50002}</td><td>{@code 429}</td><td>rate limited: no retry</td></tr>
 *   <tr><td>{@code 50003}</td><td>no answer for 10 s</td><td>the read timeout, then retries</td></tr>
 *   <tr><td>{@code 40404}</td><td>{@code 404}</td><td>refused input: no retry, will not change</td></tr>
 *   <tr><td>any other</td><td>a quote</td><td>the happy path</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/{version}/stub-carrier")
@ConditionalOnProperty(name = "app.delivery.carrier.stub.enabled", havingValue = "true")
@Hidden
class StubCarrierController {

	private static final long SLOW_MILLIS = 10_000;

	private final AtomicInteger flakyCalls = new AtomicInteger();

	@GetMapping(path = "/quotes", version = "v1")
	ResponseEntity<?> quote(@RequestParam final String postcode) throws InterruptedException {
		switch (postcode) {
			case "50000" -> {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "carrier down"));
			}
			case "50001" -> {
				if (flakyCalls.incrementAndGet() % 2 == 1) {
					return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "carrier hiccup"));
				}
			}
			case "50002" -> {
				return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "30").build();
			}
			case "50003" -> Thread.sleep(SLOW_MILLIS);
			case "40404" -> {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "no delivery to this postcode"));
			}
			default -> {
			}
		}
		final int days = 2 + (postcode.charAt(0) - '0') % 4;
		return ResponseEntity.ok(new CarrierQuoteClient.Quote(days, new BigDecimal("4.90"), "Stub Express"));
	}
}