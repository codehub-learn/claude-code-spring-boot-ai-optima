package gr.codelearn.springbootai.persistencedemo.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CourierRepository extends JpaRepository<Courier, Long> {

	Optional<Courier> findByEmployeeCode(String employeeCode);

	boolean existsByEmployeeCode(String employeeCode);

	Page<Courier> findByDepotCode(String depotCode, Pageable pageable);
}