package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import gr.codelearn.springbootai.persistencedemo.shipment.ShipmentStatus;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

public record TrackingEventRequest(ShipmentStatus status, String depotCode, OffsetDateTime occurredAt, @Nullable String note) {
}
