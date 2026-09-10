package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.3: exercises {@link ProductRepository} against the {@code R__010} baseline (6 categories, 4 products each) on real Oracle. Covers
 * derived queries, {@code @Query} (JPQL + native), {@link org.springframework.data.domain.Pageable Pageable}/{@link Sort}, dynamic
 * {@link Specification} filtering, the three projection styles, and a hard N+1 assertion for {@code @EntityGraph}.
 *
 * <p>{@code @Transactional} rolls every method back, so the shared seed is never mutated. Assertions lean on tolerant forms
 * ({@code >=}, known SKUs) except where the seed count per category is fixed at 4 (see {@code R__010_seed_catalog.sql}).
 */
@Transactional
class ProductRepositoryIT extends AbstractOracleIntegrationTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private EntityManagerFactory emf;

	private Statistics statistics;

	private Category laptops() {
		return categoryRepository.findByName("Laptops").orElseThrow();
	}

	@BeforeEach
	void enableStatistics() {
		statistics = emf.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
	}

	@Test
	void derivedQueriesResolveAgainstTheSeed() {
		assertThat(productRepository.findBySku("LAP-0001")).isPresent()
														   .get()
														   .extracting(Product::getName)
														   .isEqualTo("Latitude 14 Ultralight");
		assertThat(productRepository.existsBySku("LAP-0001")).isTrue();
		assertThat(productRepository.existsBySku("NOPE-9999")).isFalse();
		assertThat(productRepository.findByCategoryNameIgnoreCase("laptops")).hasSize(4);
		assertThat(productRepository.countByCategory(laptops())).isEqualTo(4);
		assertThat(productRepository.findByPriceBetween(new BigDecimal("1000"), new BigDecimal("1400")))
				.extracting(Product::getSku)
				.contains("LAP-0001", "LAP-0003");
	}

	@Test
	void sortIsPushedToTheDatabase() {
		final List<Product> byPriceDesc =
				productRepository.findByNameContainingIgnoreCase("proview", Sort.by(Sort.Direction.DESC, "price"));

		assertThat(byPriceDesc).extracting(Product::getPrice).isSortedAccordingTo((a, b) -> b.compareTo(a));
	}

	@Test
	void pageCarriesSliceAndTotal() {
		final var firstPage = productRepository.findByCategory(laptops(), PageRequest.of(0, 2, Sort.by("sku")));

		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(4);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getContent()).extracting(Product::getSku).containsExactly("LAP-0001", "LAP-0002");
	}

	@Test
	void explicitJpqlAndNativeQueriesRun() {
		assertThat(productRepository.findAtOrAbove(new BigDecimal("2000")))
				.extracting(Product::getSku)
				.contains("LAP-0002");

		final Long laptopsId = laptops().getId();
		assertThat(productRepository.findMostExpensiveInCategory(laptopsId))
				.extracting(Product::getSku)
				.containsExactly("LAP-0002"); // 2199.00 is the category max
	}

	@Test
	void specificationsComposeIntoOneDynamicWhere() {
		final Specification<Product> filter = Specification.allOf(
				ProductSpecifications.nameContains("pro"),
				ProductSpecifications.priceAtLeast(new BigDecimal("300")),
				ProductSpecifications.priceAtMost(new BigDecimal("900")),
				ProductSpecifications.inCategory(null)); // a blank form field drops out

		final List<Product> hits = productRepository.findAll(filter, Sort.by("sku"));

		assertThat(hits).isNotEmpty()
						.allSatisfy(p -> {
							assertThat(p.getName().toLowerCase()).contains("pro");
							assertThat(p.getPrice()).isBetween(new BigDecimal("300"), new BigDecimal("900"));
						});
	}

	@Test
	void projectionsSelectOnlyWhatTheyDeclare() {
		// interface projection
		final List<ProductSummary> summaries = productRepository.findByCategoryOrderByPriceAsc(laptops());
		assertThat(summaries).hasSize(4)
							 .extracting(ProductSummary::getPrice)
							 .isSorted();

		// dynamic projection: same query, entity vs projection by argument
		assertThat(productRepository.findBySkuStartingWith("LAP-", Product.class)).hasSize(4);
		assertThat(productRepository.findBySkuStartingWith("LAP-", ProductSummary.class))
				.hasSize(4)
				.first()
				.extracting(ProductSummary::getSku)
				.asString()
				.startsWith("LAP-");

		// record (DTO) projection via JPQL constructor expression
		assertThat(productRepository.priceView())
				.hasSize(24)
				.anySatisfy(view -> {
					assertThat(view.sku()).isEqualTo("LAP-0001");
					assertThat(view.categoryName()).isEqualTo("Laptops");
				});
	}

	@Test
	void entityGraphCollapsesTheNPlusOne() {
		em.clear();
		statistics.clear();
		productRepository.findAllWithCategory().forEach(p -> p.getCategory().getName());
		final long withGraph = statistics.getPrepareStatementCount();

		em.clear();
		statistics.clear();
		productRepository.findAll().forEach(p -> p.getCategory().getName());
		final long lazy = statistics.getPrepareStatementCount();

		assertThat(withGraph).isEqualTo(1);
		assertThat(lazy).isGreaterThan(withGraph); // one select for products + one per distinct category
	}
}