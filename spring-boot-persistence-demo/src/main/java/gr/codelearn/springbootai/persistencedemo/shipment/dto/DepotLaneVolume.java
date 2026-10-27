package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import java.math.BigDecimal;

/**
 * Shipment volume on one origin to destination depot lane. Mapped straight from the columns of {@code ShipmentReportRepository} by
 * {@code JdbcClient}, which matches column names to record components (snake_case to camelCase).
 */
public record DepotLaneVolume(String originDepotCode, String destinationDepotCode, long shipments, BigDecimal avgWeightKg) {
}
