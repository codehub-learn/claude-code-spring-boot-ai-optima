package gr.codelearn.springbootai.persistencedemo.shipment.mapper;

import gr.codelearn.springbootai.persistencedemo.shipment.Shipment;
import gr.codelearn.springbootai.persistencedemo.shipment.TrackingEvent;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ShipmentDetailResponse;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ShipmentResponse;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.TrackingEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

	@Mapping(target = "customerCode", source = "customer.customerCode")
	@Mapping(target = "originDepotCode", source = "originDepot.code")
	@Mapping(target = "destinationDepotCode", source = "destinationDepot.code")
	@Mapping(target = "courierCode", source = "courier.employeeCode")
	@Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
	@Mapping(target = "recipientLine1", source = "recipientAddress.line1")
	@Mapping(target = "recipientCity", source = "recipientAddress.city")
	@Mapping(target = "recipientPostalCode", source = "recipientAddress.postalCode")
	ShipmentResponse toResponse(Shipment shipment);

	@Mapping(target = "depotCode", source = "depot.code")
	TrackingEventResponse toEventResponse(TrackingEvent event);

	/**
	 * The shipment itself ({@code source = "."}) plus its events, mapped through {@link #toResponse} and {@link #toEventResponse}.
	 */
	@Mapping(target = "shipment", source = ".")
	ShipmentDetailResponse toDetailResponse(Shipment shipment);
}