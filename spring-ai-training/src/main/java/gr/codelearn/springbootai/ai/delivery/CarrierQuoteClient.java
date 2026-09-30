package gr.codelearn.springbootai.ai.delivery;

import gr.codelearn.springbootai.ai.common.ExternalServiceException;

import java.math.BigDecimal;

/**
 * Unit 11.3: the carrier service, seen from the application: ask for a quote, get one or an {@link ExternalServiceException} that says what
 * to do about the failure. The only implementation talks HTTP ({@link HttpCarrierQuoteClient}); an interface so the tests can put a failing
 * carrier in its place.
 */
interface CarrierQuoteClient {

	/**
	 * The carrier's quote for delivering to {@code postcode}.
	 *
	 * @throws ExternalServiceException the carrier could not give one, classified by {@link ExternalServiceException.Kind}
	 */
	Quote quote(String postcode);

	/**
	 * A carrier's answer.
	 *
	 * @param days    working days until delivery
	 * @param cost    the delivery cost in EUR
	 * @param carrier who delivers
	 */
	record Quote(int days, BigDecimal cost, String carrier) {
	}
}