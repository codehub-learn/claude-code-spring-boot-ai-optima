package gr.codelearn.springbootai.ai.delivery;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Unit 11.3: wires the carrier client with the timeouts from {@link DeliveryProperties}. The connect and read timeouts are what turn a
 * carrier that hangs into an {@code UNAVAILABLE} answer after a couple of seconds instead of a tool call that never returns.
 */
@Configuration
class DeliveryConfig {

	@Bean
	CarrierQuoteClient carrierQuoteClient(final RestClient.Builder builder, final DeliveryProperties properties) {
		final var settings = HttpClientSettings.defaults().withTimeouts(properties.connectTimeout(), properties.readTimeout());
		final RestClient restClient = builder.baseUrl(properties.baseUrl().toString())
											 .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings))
											 .build();
		return new HttpCarrierQuoteClient(restClient, properties);
	}
}
