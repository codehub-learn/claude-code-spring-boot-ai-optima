package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.delivery.dto.DeliveryEstimate;
import gr.codelearn.springbootai.ai.product.ProductService;
import gr.codelearn.springbootai.ai.product.dto.ProductResponse;

import org.springframework.stereotype.Service;

/**
 * Unit 11.3: a delivery estimate for a catalog product: the product is looked up first (an unknown SKU is a fact, not a carrier problem),
 * then the carrier is asked. Deliberately not {@code @Transactional}: the carrier call is a network call, and CLAUDE.md keeps network and
 * AI calls out of transactions.
 */
@Service
public class DeliveryService {

	private final ProductService productService;
	private final CarrierQuoteClient carrier;

	DeliveryService(final ProductService productService, final CarrierQuoteClient carrier) {
		this.productService = productService;
		this.carrier = carrier;
	}

	/**
	 * @throws gr.codelearn.springbootai.ai.product.ProductNotFoundException no product has this SKU
	 * @throws gr.codelearn.springbootai.ai.common.ExternalServiceException  the carrier could not give a quote
	 */
	public DeliveryEstimate estimate(final String sku, final String postcode) {
		final ProductResponse product = productService.get(sku);
		final CarrierQuoteClient.Quote quote = carrier.quote(postcode);
		return new DeliveryEstimate(product.sku(), postcode, quote.days(), quote.cost(), quote.carrier());
	}
}