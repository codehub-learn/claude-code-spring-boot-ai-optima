package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ExternalServiceException.Kind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit 11.3: the HTTP client of the carrier service. Its job is to turn every way the call can go wrong into an
 * {@link ExternalServiceException} with a message safe for a model, and to retry only what is worth retrying:
 *
 * <table>
 *   <caption>What each outcome becomes</caption>
 *   <tr><th>Carrier answers</th><th>Kind</th><th>Retried</th></tr>
 *   <tr><td>connection refused, timeout, {@code 5xx}</td><td>{@code UNAVAILABLE}</td><td>yes, up to {@code maxRetries} more times</td></tr>
 *   <tr><td>{@code 429}</td><td>{@code RATE_LIMITED}</td><td>no: asking again at once makes it worse</td></tr>
 *   <tr><td>{@code 404}, any other {@code 4xx}</td><td>{@code REJECTED}</td><td>no: the same input gets the same answer</td></tr>
 * </table>
 *
 * <p>Retries use Spring Framework 7's own {@link RetryTemplate} ({@code org.springframework.core.retry}), not the Spring Retry library.
 * The carrier is only read, so asking again is safe; a call that changed something would need an idempotency key first. The details
 * (URL, status code, the underlying exception) go to the log; the message a model reads has none of them.
 */
final class HttpCarrierQuoteClient implements CarrierQuoteClient {

	private static final Logger log = LoggerFactory.getLogger(HttpCarrierQuoteClient.class);

	private final RestClient restClient;
	private final RetryTemplate retryTemplate;

	HttpCarrierQuoteClient(final RestClient restClient, final DeliveryProperties properties) {
		this.restClient = restClient;
		this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
														  .maxRetries(properties.maxRetries())
														  .delay(properties.retryDelay())
														  .predicate(HttpCarrierQuoteClient::isUnavailable)
														  .build());
	}

	@Override
	public Quote quote(final String postcode) {
		final AtomicInteger attempts = new AtomicInteger();
		try {
			return retryTemplate.invoke(() -> {
				attempts.incrementAndGet();
				return ask(postcode);
			});
		} catch (ExternalServiceException ex) {
			if (ex.kind() == Kind.UNAVAILABLE) {
				throw new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service is not responding (%d %s)".formatted(
						attempts.get(), attempts.get() == 1 ? "attempt" : "attempts"), ex.getCause());
			}
			throw ex;
		}
	}

	private Quote ask(final String postcode) {
		try {
			final Quote quote = restClient.get().uri("/quotes?postcode={postcode}", postcode).retrieve().body(Quote.class);
			if (quote == null) {
				throw new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service answered without a quote", null);
			}
			return quote;
		} catch (HttpClientErrorException.TooManyRequests ex) {
			log.warn("Carrier rate limit hit: {}", ex.getMessage());
			throw new ExternalServiceException(Kind.RATE_LIMITED, "The delivery service is busy (too many requests)", ex);
		} catch (HttpClientErrorException ex) {
			log.info("Carrier refused postcode {}: {}", postcode, ex.getStatusCode());
			throw new ExternalServiceException(Kind.REJECTED, "The carrier does not deliver to postcode " + postcode, ex);
		} catch (HttpServerErrorException | ResourceAccessException ex) {
			log.warn("Carrier unavailable: {}", ex.getMessage());
			throw new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service did not answer", ex);
		} catch (RestClientException ex) {
			// An answer that cannot be read as a quote (bad JSON, unexpected content type): the carrier misbehaves, retrying may not help.
			log.warn("Carrier answer not understood: {}", ex.getMessage());
			throw new ExternalServiceException(Kind.REJECTED, "The delivery service gave an answer that could not be understood", ex);
		}
	}

	private static boolean isUnavailable(final Throwable throwable) {
		return throwable instanceof ExternalServiceException ex && ex.kind() == Kind.UNAVAILABLE;
	}
}
