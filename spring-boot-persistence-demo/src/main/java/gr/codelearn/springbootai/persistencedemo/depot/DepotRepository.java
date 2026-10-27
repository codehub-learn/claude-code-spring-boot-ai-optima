package gr.codelearn.springbootai.persistencedemo.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface DepotRepository extends JpaRepository<Depot, Long> {

	Optional<Depot> findByCode(String code);

	boolean existsByCode(String code);
}