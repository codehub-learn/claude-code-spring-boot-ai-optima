package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import java.util.List;

/**
 * A shipment together with its scan history, oldest first.
 */
public record ShipmentDetailResponse(ShipmentResponse shipment, List<TrackingEventResponse> events) {
}