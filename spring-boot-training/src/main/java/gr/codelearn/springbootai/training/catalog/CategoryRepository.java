package gr.codelearn.springbootai.training.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for the {@link Category} aggregate. {@link JpaRepository} already supplies {@code save}, {@code findById}, {@code findAll},
 * {@code count}, {@code delete}, paging and sorting — only the queries specific to this aggregate are declared here, and Spring Data
 * derives their implementation from the method name at startup (a bad name fails the context, not a call).
 *
 * <p>The interface is package-private: nothing outside {@code catalog} touches it yet (services arrive in 3.4). Spring Data still creates
 * the proxy.
 */
interface CategoryRepository extends JpaRepository<Category, Long> {

	/**
	 * Derived query on the {@code name} business key — {@code where name = ?1}.
	 */
	Optional<Category> findByName(String name);

	/**
	 * Derived existence check — {@code select count(...) > 0}, no entity materialised.
	 */
	boolean existsByName(String name);
}