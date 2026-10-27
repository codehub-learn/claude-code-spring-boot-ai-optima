package gr.codelearn.springbootai.persistencedemo.shipment;

import gr.codelearn.springbootai.persistencedemo.common.Address;
import gr.codelearn.springbootai.persistencedemo.common.ConflictException;
import gr.codelearn.springbootai.persistencedemo.common.NotFoundException;
import gr.codelearn.springbootai.persistencedemo.customer.CustomerService;
import gr.codelearn.springbootai.persistencedemo.depot.DepotService;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ShipmentBookRequest;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ShipmentDetailResponse;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ShipmentResponse;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.TrackingEventRequest;
import gr.codelearn.springbootai.persistencedemo.shipment.mapper.ShipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for {@link Shipment}, the aggregate root of its {@link TrackingEvent}s: booking, lookup with the scan history, and recording
 * scans. Customer and depots are resolved through their own services.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ShipmentService {

	private final ShipmentRepository shipmentRepository;
	private final ShipmentMapper shipmentMapper;
	private final CustomerService customerService;
	private final DepotService depotService;

	Page<ShipmentResponse> findByCustomer(final String customerCode, final Pageable pageable) {
		return shipmentRepository.findByCustomerCustomerCode(customerCode, pageable).map(shipmentMapper::toResponse);
	}

	Page<ShipmentResponse> findByStatus(final ShipmentStatus status, final Pageable pageable) {
		return shipmentRepository.findByStatus(status, pageable).map(shipmentMapper::toResponse);
	}

	ShipmentDetailResponse getByTrackingNumber(final String trackingNumber) {
		return shipmentMapper.toDetailResponse(requireWithEvents(trackingNumber));
	}

	@Transactional
	ShipmentDetailResponse book(final ShipmentBookRequest request) {
		if (shipmentRepository.existsByTrackingNumber(request.trackingNumber())) {
			throw new ConflictException("Shipment '%s' already exists".formatted(request.trackingNumber()));
		}
		final Shipment shipment = new Shipment(request.trackingNumber(), customerService.requireByCode(request.customerCode()),
				depotService.requireByCode(request.originDepotCode()),
				depotService.requireByCode(request.destinationDepotCode()), request.serviceLevel(),
				request.weightKg(), request.price(), request.recipientName(),
				new Address(request.recipientLine1(), request.recipientCity(), request.recipientPostalCode()),
				request.promisedBy());
		return shipmentMapper.toDetailResponse(shipmentRepository.save(shipment));
	}

	/**
	 * Appends a scan and keeps the denormalised shipment columns in step: {@code pickedUpAt}, the {@code attempts} counter and
	 * {@code deliveredAt}. The new event is persisted by cascade when the managed shipment flushes at commit.
	 */
	@Transactional
	ShipmentDetailResponse recordEvent(final String trackingNumber, final TrackingEventRequest request) {
		final Shipment shipment = requireWithEvents(trackingNumber);
		shipment.addEvent(request.status(), depotService.requireByCode(request.depotCode()), request.occurredAt(), request.note());
		switch (request.status()) {
			case PICKED_UP -> shipment.setPickedUpAt(request.occurredAt());
			case OUT_FOR_DELIVERY -> shipment.setAttempts(shipment.getAttempts() + 1);
			case DELIVERED -> shipment.setDeliveredAt(request.occurredAt());
			case CREATED -> throw new ConflictException("A tracking event cannot have status CREATED");
			case AT_HUB, FAILED_ATTEMPT -> {
			}
		}
		return shipmentMapper.toDetailResponse(shipment);
	}

	private Shipment requireWithEvents(final String trackingNumber) {
		return shipmentRepository.findWithEventsByTrackingNumber(trackingNumber)
				.orElseThrow(() -> new NotFoundException("Shipment", trackingNumber));
	}
}
