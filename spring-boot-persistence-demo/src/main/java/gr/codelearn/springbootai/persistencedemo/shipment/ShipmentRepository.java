package gr.codelearn.springbootai.persistencedemo.shipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ShipmentRepository extends JpaRepository<Shipment, Long> {

	boolean existsByTrackingNumber(String trackingNumber);

	/**
	 * Loads the shipment together with its scan history in one query ({@code events} is lazy by default).
	 */
	@EntityGraph(attributePaths = "events")
	Optional<Shipment> findWithEventsByTrackingNumber(String trackingNumber);

	Page<Shipment> findByCustomerCustomerCode(String customerCode, Pageable pageable);

	Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}