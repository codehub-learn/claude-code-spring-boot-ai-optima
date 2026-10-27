package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import gr.codelearn.springbootai.persistencedemo.shipment.ServiceLevel;
import gr.codelearn.springbootai.persistencedemo.shipment.ShipmentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

public record ShipmentResponse(String trackingNumber, String customerCode, String originDepotCode, String destinationDepotCode,
							   @Nullable String courierCode, @Nullable String invoiceNumber, ServiceLevel serviceLevel,
							   ShipmentStatus status, BigDecimal weightKg, BigDecimal price, String recipientName,
							   String recipientLine1, String recipientCity, String recipientPostalCode,
							   @Nullable OffsetDateTime pickedUpAt, OffsetDateTime promisedBy, @Nullable OffsetDateTime deliveredAt,
							   int attempts) {
}
