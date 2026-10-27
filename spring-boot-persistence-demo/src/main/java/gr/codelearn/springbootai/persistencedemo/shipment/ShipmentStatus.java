package gr.codelearn.springbootai.persistencedemo.shipment;

/**
 * Where a {@link Shipment} is in its lifecycle. Also the status of a {@link TrackingEvent}, which can take every value except
 * {@code CREATED} (a shipment is created before its first scan). Persisted as its {@code name()}; {@code ck_shipment_status} and
 * {@code ck_tracking_event_status} pin the columns to these names.
 */
public enum ShipmentStatus {
	CREATED,
	PICKED_UP,
	AT_HUB,
	OUT_FOR_DELIVERY,
	DELIVERED,
	FAILED_ATTEMPT
}