package gr.codelearn.springbootai.training.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for the {@link Product} aggregate — the topic's main exhibit. It carries one example of every query style the curriculum
 * covers:
 * <ul>
 *   <li><b>derived queries</b> — method name parsed into a {@code where} clause at startup;</li>
 *   <li><b>{@code @Query}</b> — explicit JPQL and native SQL when a derived name would be unwieldy or impossible;</li>
 *   <li><b>{@link Pageable} / {@link Sort}</b> — slicing and ordering pushed to the database;</li>
 *   <li><b>{@link JpaSpecificationExecutor}</b> — dynamic, composable {@code where} clauses (see {@link ProductSpecifications});</li>
 *   <li><b>{@link EntityGraph}</b> — eager-load an association in the same select to kill an N+1;</li>
 *   <li><b>projections</b> — interface ({@link ProductSummary}), record ({@link ProductPriceView}) and dynamic ({@code Class<T>}).</li>
 * </ul>
 */
interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// ---- derived queries -------------------------------------------------------------------------------------------------------------

	/**
	 * {@code where sku = ?1} — the business key.
	 */
	Optional<Product> findBySku(String sku);

	/**
	 * {@code select count(...) > 0} — cheaper than fetching when you only need existence.
	 */
	boolean existsBySku(String sku);

	/**
	 * Traverses the {@code product → category} association by property path: {@code where lower(category.name) = lower(?1)}.
	 */
	List<Product> findByCategoryNameIgnoreCase(String categoryName);

	/**
	 * {@code where price between ?1 and ?2} (inclusive).
	 */
	List<Product> findByPriceBetween(BigDecimal minInclusive, BigDecimal maxInclusive);

	/**
	 * {@code select count(*) where category = ?1} — counts without loading rows.
	 */
	long countByCategory(Category category);

	/**
	 * {@code where name like %?1%} with a caller-supplied {@link Sort} appended to the {@code order by}.
	 */
	List<Product> findByNameContainingIgnoreCase(String fragment, Sort sort);

	// ---- Pageable ------------------------------------------------------------------------------------------------------------------

	/**
	 * A {@link Page} runs two statements: the slice ({@code where category = ?1} + {@code offset/fetch}) and a {@code count(*)} for
	 * {@link Page#getTotalElements()}. Use {@code Slice} instead when the total is not needed.
	 */
	Page<Product> findByCategory(Category category, Pageable pageable);

	// ---- @Query: JPQL and native --------------------------------------------------------------------------------------------------

	/**
	 * Explicit JPQL. Equivalent to a derived {@code findByPriceGreaterThanEqualOrderByPriceDesc}, but the intent reads better spelled out.
	 */
	@Query("select p from Product p where p.price >= :min order by p.price desc")
	List<Product> findAtOrAbove(@Param("min") BigDecimal min);

	/**
	 * Native Oracle SQL: every product whose price equals the maximum price in its category. The correlated-subquery shape is awkward in
	 * JPQL and the query is read-only reporting, so raw SQL is the pragmatic choice — at the cost of portability (this is
	 * Oracle-flavoured)
	 * and no compile-time mapping check.
	 */
	@Query(value = """
				   select * from product
				   where price = (select max(price) from product where category_id = :categoryId)
				     and category_id = :categoryId
				   """, nativeQuery = true)
	List<Product> findMostExpensiveInCategory(@Param("categoryId") Long categoryId);

	// ---- @EntityGraph: the N+1 fix ------------------------------------------------------------------------------------------------

	/**
	 * {@code product.category} is {@code fetch = LAZY}, so iterating {@link #findAll()} and touching {@code getCategory().getName()} fires
	 * one extra select per product (the classic N+1). This method attaches a fetch graph, so Hibernate loads every category in the same
	 * join and the loop runs one statement total. Verified in {@code ProductRepositoryIT} via Hibernate's statement counter.
	 */
	@EntityGraph(attributePaths = "category")
	@Query("select p from Product p")
	List<Product> findAllWithCategory();

	// ---- projections --------------------------------------------------------------------------------------------------------------

	/**
	 * Closed interface projection — selects only {@code sku}/{@code name}/{@code price}, builds no entity.
	 */
	List<ProductSummary> findByCategoryOrderByPriceAsc(Category category);

	/**
	 * Dynamic projection: the same derived query returns {@code Product} when called with {@code Product.class} and a
	 * {@link ProductSummary} proxy when called with {@code ProductSummary.class}. One method, caller picks the shape.
	 */
	<T> List<T> findBySkuStartingWith(String skuPrefix, Class<T> type);

	/**
	 * DTO projection via a JPQL constructor expression — see {@link ProductPriceView}.
	 */
	@Query("""
		   select new gr.codelearn.springbootai.training.catalog.ProductPriceView(p.sku, p.name, c.name, p.price)
		   from Product p join p.category c
		   order by p.sku
		   """)
	List<ProductPriceView> priceView();
}