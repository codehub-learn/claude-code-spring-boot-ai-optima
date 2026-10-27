package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import gr.codelearn.springbootai.persistencedemo.shipment.ServiceLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Books a shipment. The tracking number, price and promised time are supplied by the caller; computing them is a pricing concern
 * that comes later.
 */
public record ShipmentBookRequest(String trackingNumber, String customerCode, String originDepotCode, String destinationDepotCode,
								  ServiceLevel serviceLevel, BigDecimal weightKg, BigDecimal price, String recipientName,
								  String recipientLine1, String recipientCity, String recipientPostalCode, OffsetDateTime promisedBy) {
}
