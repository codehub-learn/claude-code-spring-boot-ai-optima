package gr.codelearn.springbootai.persistencedemo.shipment.dto;

/**
 * Interface-based projection over the native ranking query in {@code ShipmentRepository}; getters match the quoted column aliases.
 */
public interface CourierDeliveryRank {

	String getDepotCode();

	String getEmployeeCode();

	String getLastName();

	long getDelivered();

	int getDepotRank();
}