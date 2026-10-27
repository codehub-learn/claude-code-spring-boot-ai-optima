package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import java.math.BigDecimal;

/**
 * Class-based projection: filled by the JPQL constructor expression {@code select new ...CustomerRevenueSummary(...)} in
 * {@code ShipmentRepository}.
 */
public record CustomerRevenueSummary(String customerCode, long shipments, BigDecimal revenue) {
}