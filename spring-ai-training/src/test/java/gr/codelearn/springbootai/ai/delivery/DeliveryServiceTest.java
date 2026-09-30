package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;
import gr.codelearn.springbootai.ai.common.ExternalServiceException.Kind;
import gr.codelearn.springbootai.ai.delivery.dto.DeliveryEstimate;
import gr.codelearn.springbootai.ai.product.ProductService;
import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Unit 11.3: the estimate combines the catalog and the carrier, in that order, and lets the carrier's classified failures through.
 */
class DeliveryServiceTest {

	private static final ProductResponse LAPTOP =
			new ProductResponse("LAP-0002", "Latitude 16 Workstation", "Laptops", new BigDecimal("2199.00"), "EUR", null);

	private final ProductService productService = mock(ProductService.class);
	private final CarrierQuoteClient carrier = mock(CarrierQuoteClient.class);
	private final DeliveryService service = new DeliveryService(productService, carrier);

	@Test
	void theEstimateIsTheCarriersQuoteForTheCatalogProduct() {
		given(productService.get("LAP-0002")).willReturn(LAPTOP);
		given(carrier.quote("10431")).willReturn(new CarrierQuoteClient.Quote(3, new BigDecimal("4.90"), "Stub Express"));

		assertThat(service.estimate("LAP-0002", "10431")).isEqualTo(
				new DeliveryEstimate("LAP-0002", "10431", 3, new BigDecimal("4.90"), "Stub Express"));
	}

	@Test
	void anUnknownProductIsNotWorthACarrierCall() {
		given(productService.get("AUD-9999")).willThrow(new IllegalArgumentException("stands in for ProductNotFoundException"));

		assertThatThrownBy(() -> service.estimate("AUD-9999", "10431")).isInstanceOf(IllegalArgumentException.class);

		then(carrier).shouldHaveNoInteractions();
	}

	@Test
	void aCarrierFailureReachesTheCallerAsItIs() {
		given(productService.get("LAP-0002")).willReturn(LAPTOP);
		given(carrier.quote("50000")).willThrow(new ExternalServiceException(Kind.UNAVAILABLE, "The delivery service is down", null));

		assertThatThrownBy(() -> service.estimate("LAP-0002", "50000")).isInstanceOfSatisfying(ExternalServiceException.class,
																							   ex -> assertThat(ex.kind()).isEqualTo(
				                                                                                       Kind.UNAVAILABLE));
	}
}