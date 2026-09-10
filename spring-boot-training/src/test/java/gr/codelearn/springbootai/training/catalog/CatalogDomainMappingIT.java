package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.2: proves the domain graph maps, persists and reloads correctly against real Oracle. Every other {@code @SpringBootTest} in the module
 * already forces {@code ddl-auto=validate} to accept the five new entity mappings against {@code V1}/{@code V2} (the context would not
 * start otherwise); this test adds behavioural coverage for the associations, cascade + {@code orphanRemoval}, {@code @Version} and JPA
 * auditing.
 *
 * <p>{@code @Transactional} rolls every method back, so the seeded rows ({@code R__010}/{@code R__020}) are untouched. The persistence
 * context stays open for the whole method, so lazy navigation and {@code flush}/{@code clear} round-trips work.
 */
@Transactional
class CatalogDomainMappingIT extends AbstractOracleIntegrationTest {

	@PersistenceContext
	private EntityManager em;

	private Product seededProduct(final String sku) {
		return em.createQuery("select p from Product p where p.sku = :sku", Product.class)
				 .setParameter("sku", sku)
				 .getSingleResult();
	}

	private Order newOrderWithTwoLines(final String orderNumber) {
		final Address address = new Address("1 Test Way", null, "Testville", "00001", "US");
		final Customer customer = new Customer(orderNumber + "@example.com", "Test", "Buyer", address);
		em.persist(customer);

		final Order order = new Order(orderNumber, customer, OffsetDateTime.now(), address);
		order.addItem(seededProduct("LAP-0001"), 1, new BigDecimal("1299.00"));
		order.addItem(seededProduct("AUD-0002"), 2, new BigDecimal("229.00"));
		em.persist(order);
		return order;
	}

	@Test
	void persistsAndReloadsTheWholeGraph() {
		final Long id = newOrderWithTwoLines("IT-0001").getId();
		em.flush();
		em.clear();

		final Order reloaded = em.find(Order.class, id);
		assertThat(reloaded.getItems()).hasSize(2);
		assertThat(reloaded.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1757.00")); // 1*1299 + 2*229
		// lazy @ManyToOne resolves while the context is open
		assertThat(reloaded.getCustomer().getEmail()).isEqualTo("IT-0001@example.com");
		assertThat(reloaded.getItems())
				.extracting(item -> item.getProduct().getSku())
				.containsExactlyInAnyOrder("LAP-0001", "AUD-0002");
		assertThat(reloaded.getShippingAddress().city()).isEqualTo("Testville");
	}

	@Test
	void auditingFieldsAndVersionArePopulatedOnInsert() {
		final Order order = newOrderWithTwoLines("IT-0002");
		em.flush();

		assertThat(order.getCreatedAt()).isNotNull();
		assertThat(order.getUpdatedAt()).isNotNull();
		assertThat(order.getVersion()).isZero();
	}

	@Test
	void versionIncrementsOnUpdate() {
		final Order order = newOrderWithTwoLines("IT-0003");
		em.flush();
		final long initialVersion = order.getVersion();

		order.setStatus(OrderStatus.SHIPPED);
		em.flush();

		assertThat(order.getVersion()).isEqualTo(initialVersion + 1);
	}

	@Test
	void orphanRemovalDeletesTheLineRow() {
		final Order order = newOrderWithTwoLines("IT-0004");
		em.flush();
		final Long orderId = order.getId();

		final OrderItem toDrop = order.getItems().iterator().next();
		order.removeItem(toDrop);
		em.flush();

		final Number remaining = (Number) em.createNativeQuery(
													"select count(*) from order_item where order_id = :id")
											.setParameter("id", orderId)
											.getSingleResult();
		assertThat(remaining.intValue()).isEqualTo(1);
	}
}