package gr.codelearn.springbootai.persistencedemo.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CourierRepository extends JpaRepository<Courier, Long> {

	Optional<Courier> findByEmployeeCode(String employeeCode);

	boolean existsByEmployeeCode(String employeeCode);

	Page<Courier> findByDepotCode(String depotCode, Pageable pageable);

	/**
	 * Active couriers of a depot that can carry at least {@code minCapacity} parcels a day, biggest capacity first. The depot is
	 * fetched in
	 * the same query because the response shows its code.
	 */
	@Query("""
		   select c from Courier c join fetch c.depot d
		   where d.code = :depotCode
		   		   and c.active = true
		   		   and c.dailyCapacity >= :minCapacity
		   order by c.dailyCapacity desc, c.lastName""")
	List<Courier> findActiveWithCapacity(@Param("depotCode") String depotCode, @Param("minCapacity") int minCapacity);

	@Query("select c.vehicleType, count(c) from Courier c where c.active = true group by c.vehicleType")
	List<Object[]> countActiveByVehicleType();
}