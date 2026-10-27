package gr.codelearn.springbootai.persistencedemo.shipment;

import gr.codelearn.springbootai.persistencedemo.shipment.dto.CustomerRevenueSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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

	/**
	 * Shipments still undelivered after their promised time, most overdue first. The customer is fetched for the response; the count query
	 * is explicit because Spring Data cannot derive one from a {@code join fetch}.
	 */
	@Query(value = "select s from Shipment s join fetch s.customer where s.deliveredAt is null and s.promisedBy < :now order by s" +
				   ".promisedBy",
		   countQuery = "select count(s) from Shipment s where s.deliveredAt is null and s.promisedBy < :now")
	Page<Shipment> findOverdue(@Param("now") OffsetDateTime now, Pageable pageable);

	@Query("""
		   select s from Shipment s
		   where s.customer.customerCode = :customerCode and s.status = :status and s.deliveredAt between :from and :to
		   order by s.deliveredAt desc""")
	List<Shipment> findDeliveredBetween(@Param("customerCode") String customerCode, @Param("status") ShipmentStatus status,
										@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

	@Query("select s.status, count(s) from Shipment s where s.destinationDepot.code = :depotCode group by s.status")
	List<Object[]> countByStatusForDestinationDepot(@Param("depotCode") String depotCode);

	/**
	 * Revenue of a customer's delivered shipments in the period; zero when there are none.
	 */
	@Query("""
		   select coalesce(sum(s.price), 0) from Shipment s
		   where s.customer.customerCode = :customerCode and s.status = :status and s.deliveredAt >= :from and s.deliveredAt < :to""")
	BigDecimal sumPriceDelivered(@Param("customerCode") String customerCode, @Param("status") ShipmentStatus status,
								 @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

	/**
	 * Top customers by delivered revenue in the period, mapped to a record with a JPQL constructor expression (class-based projection).
	 */
	@Query("""
		   select new gr.codelearn.springbootai.persistencedemo.shipment.dto.CustomerRevenueSummary(c.customerCode, count(s), sum(s.price))
		   from Shipment s join s.customer c
		   where s.status = :status and s.deliveredAt >= :from and s.deliveredAt < :to
		   group by c.customerCode
		   order by sum(s.price) desc""")
	List<CustomerRevenueSummary> findTopCustomersByRevenue(@Param("status") ShipmentStatus status, @Param("from") OffsetDateTime from,
														   @Param("to") OffsetDateTime to, Pageable pageable);
}