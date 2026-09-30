package gr.codelearn.springbootai.ai.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * How the application talks to the carrier service, bound from {@code app.delivery.carrier.*}. Timeouts and retries are the resilience of
 * an external call (CLAUDE.md): without a timeout a hung carrier would hold the model's tool loop, and the customer, indefinitely.
 *
 * @param baseUrl        the carrier's base URL; in production an environment variable, locally the stub carrier of this application
 * @param connectTimeout how long to wait for a connection
 * @param readTimeout    how long to wait for the answer once connected
 * @param maxRetries     how many times to ask again after an unavailable answer (so {@code maxRetries + 1} attempts)
 * @param retryDelay     the pause between attempts
 */
@Validated
@ConfigurationProperties("app.delivery.carrier")
record DeliveryProperties(@NotNull URI baseUrl, @DefaultValue("1s") @NotNull Duration connectTimeout,
						  @DefaultValue("2s") @NotNull Duration readTimeout, @DefaultValue("2") @PositiveOrZero int maxRetries,
						  @DefaultValue("200ms") @NotNull Duration retryDelay) {
}