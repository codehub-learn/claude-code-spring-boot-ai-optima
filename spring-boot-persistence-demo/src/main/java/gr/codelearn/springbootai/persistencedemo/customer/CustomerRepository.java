package gr.codelearn.springbootai.persistencedemo.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByCustomerCode(String customerCode);

	boolean existsByCustomerCode(String customerCode);

	boolean existsByEmail(String email);

	Page<Customer> findByCustomerType(CustomerType customerType, Pageable pageable);
}
