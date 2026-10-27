package gr.codelearn.springbootai.persistencedemo.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByCustomerCode(String customerCode);

	boolean existsByCustomerCode(String customerCode);

	boolean existsByEmail(String email);

	Page<Customer> findByCustomerType(CustomerType customerType, Pageable pageable);

	/**
	 * Case-insensitive contains-search over the company name and the person's first and last name.
	 */
	@Query("""
		   select c from Customer c
		   where lower(c.companyName) like lower(concat('%', :term, '%'))
		      or lower(c.lastName) like lower(concat('%', :term, '%'))
		      or lower(c.firstName) like lower(concat('%', :term, '%'))""")
	Page<Customer> search(@Param("term") String term, Pageable pageable);

	@Query("select c from Customer c where c.customerType = :type and c.contractTier = :tier and c.depot.code = :depotCode")
	Page<Customer> findByTypeTierAndDepot(@Param("type") CustomerType type, @Param("tier") ContractTier tier,
										  @Param("depotCode") String depotCode, Pageable pageable);
}