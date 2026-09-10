package gr.codelearn.springbootai.training.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link Customer} aggregate. Shows a derived query that <b>traverses an {@code @Embedded} value object</b>:
 * {@code findByBillingAddressCountry} walks {@code customer.billingAddress.country}, which Spring Data resolves to the {@code country}
 * column on the {@code customer} table (embeddables have no table of their own).
 */
interface CustomerRepository extends JpaRepository<Customer, Long> {

	/**
	 * Derived query on the {@code email} business key.
	 */
	Optional<Customer> findByEmail(String email);

	/**
	 * Derived query into the embedded {@link Address}: {@code where billing_address.country = ?1}.
	 */
	List<Customer> findByBillingAddressCountry(String country);
}