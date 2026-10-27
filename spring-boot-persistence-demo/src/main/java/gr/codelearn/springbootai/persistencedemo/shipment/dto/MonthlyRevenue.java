package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import java.math.BigDecimal;

/**
 * Interface-based projection over the native monthly revenue query in {@code ShipmentRepository}.
 */
public interface MonthlyRevenue {

	/**
	 * Calendar month as {@code YYYY-MM}.
	 */
	String getMonth();

	long getShipments();

	BigDecimal getRevenue();
}