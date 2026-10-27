package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Vehicle a {@link Courier} drives. Persisted as its {@code name()}; {@code ck_courier_vehicle_type} pins the column to these names.
 */
enum VehicleType {
	MOTORBIKE,
	CARGO_BIKE,
	VAN,
	TRUCK
}