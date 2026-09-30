package gr.codelearn.springbootai.ai.delivery.dto;

import java.math.BigDecimal;

/**
 * Unit 11.3: what delivering a product to a postcode takes, as the carrier quoted it.
 *
 * @param sku      the product
 * @param postcode the delivery postcode
 * @param days     working days until delivery
 * @param cost     the delivery cost in EUR
 * @param carrier  who delivers
 */
public record DeliveryEstimate(String sku, String postcode, int days, BigDecimal cost, String carrier) {
}
