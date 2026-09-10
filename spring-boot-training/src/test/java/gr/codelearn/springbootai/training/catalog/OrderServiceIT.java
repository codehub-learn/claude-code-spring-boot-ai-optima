package gr.codelearn.springbootai.training.catalog;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * U3.4: transaction behaviour of {@link OrderService} against real Oracle. Deliberately <b>not</b> {@code @Transactional} — a test-managed
 * transaction would wrap (and roll back) everything, hiding the very commit / {@code REQUIRES_NEW} / rollback semantics under test. Instead
 * every order number is prefixed {@code TX-} and {@link #cleanUp()} deletes those rows after each test.
 *
 * <p>Customers ({@code alice@example.com}, …) and products ({@code LAP-0001}, …) come from the {@code R__010}/{@code R__020} baseline that
 * the {@code test} profile's Flyway applies to the container.
 */
class OrderServiceIT extends AbstractOracleIntegrationTest {

	private static final Address SHIP_TO = new Address("1 Test Way", null, "Testville", "00001", "US");

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderNotifications orderNotifications;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void resetListener() {
		orderNotifications.reset();
	}

	@AfterEach
	void cleanUp() {
		jdbcClient.sql("""
					   delete from order_item
					   where order_id in (select id from "order" where order_number like 'TX-%')
					   """).update();
		jdbcClient.sql("delete from \"order\" where order_number like 'TX-%'").update();
	}

	private PlaceOrderCommand command(final String orderNumber, final String email, final PlaceOrderCommand.Line... lines) {
		return new PlaceOrderCommand(orderNumber, email, SHIP_TO, List.of(lines));
	}

	private String txNumber(final String tag) {
		return "TX-" + tag + "-" + System.nanoTime();
	}

	private long orderRows(final String orderNumber) {
		return jdbcClient.sql("select count(*) from \"order\" where order_number = ?")
						 .param(orderNumber)
						 .query(Long.class)
						 .single();
	}

	@Test
	void placeOrderCommitsTheGraphAndFiresTheAfterCommitListener() {
		final String number = txNumber("commit");

		final OrderConfirmation confirmation = orderService.placeOrder(
				command(number, "alice@example.com",
						new PlaceOrderCommand.Line("LAP-0001", 1),
						new PlaceOrderCommand.Line("AUD-0002", 2)));

		assertThat(confirmation.lineCount()).isEqualTo(2);
		assertThat(confirmation.total()).isEqualByComparingTo("1757.00"); // 1*1299 + 2*229

		assertThat(orderRows(number)).isOne();
		final BigDecimal storedTotal = jdbcClient.sql("select total_amount from \"order\" where order_number = ?")
												 .param(number)
												 .query(BigDecimal.class)
												 .single();
		assertThat(storedTotal).isEqualByComparingTo("1757.00");
		assertThat(orderNotifications.deliveredCount()).isOne();
	}

	@Test
	void runtimeExceptionRollsBackTheOrderAndTheListenerStaysSilent() {
		final String number = txNumber("rollback");

		assertThatExceptionOfType(CatalogNotFoundException.class).isThrownBy(() -> orderService.placeOrder(
				command(number, "alice@example.com",
						new PlaceOrderCommand.Line("LAP-0001", 1),
						new PlaceOrderCommand.Line("NOPE-9999", 1))));

		assertThat(orderRows(number)).isZero();
		assertThat(orderNotifications.deliveredCount()).isZero();
	}

	@Test
	void checkedExceptionWithRollbackForRollsTheOrderBack() {
		final String number = txNumber("receipt");

		assertThatThrownBy(() -> orderService.placeOrderAndDeliverReceipt(
				command(number, "bob@example.com", new PlaceOrderCommand.Line("PHN-0001", 1))))
				.isInstanceOf(ReceiptDeliveryException.class);

		assertThat(orderRows(number)).isZero();
		assertThat(orderNotifications.deliveredCount()).isZero(); // rolled back → no AFTER_COMMIT
	}

	@Test
	void noRollbackForKeepsTheCommitDespiteTheRuntimeException() {
		final String number = txNumber("lowstock");

		assertThatThrownBy(() -> orderService.placeOrderToleratingLowStock(
				command(number, "carol@example.com", new PlaceOrderCommand.Line("MON-0002", 2))))
				.isInstanceOf(LowStockException.class);

		assertThat(orderRows(number)).isOne();
		assertThat(orderNotifications.deliveredCount()).isOne(); // committed → AFTER_COMMIT ran
	}

	@Test
	void readOnlyTransactionDoesNotFlushADirtyEntity() {
		final String number = txNumber("readonly");
		orderService.placeOrder(command(number, "alice@example.com", new PlaceOrderCommand.Line("LAP-0001", 1)));

		final OrderStatus inMemory = orderService.peekStatusAfterLocalEdit(number, OrderStatus.CANCELLED);
		assertThat(inMemory).isEqualTo(OrderStatus.CANCELLED); // the entity changed in memory

		final String stored = jdbcClient.sql("select status from \"order\" where order_number = ?")
										.param(number)
										.query(String.class)
										.single();
		assertThat(stored).isEqualTo("NEW"); // ... but the read-only transaction never wrote it
	}

	@Test
	void requiresNewLetsAGoodOrderSurviveABadOneInTheSameBatch() {
		final String good1 = txNumber("batch-good1");
		final String poison = txNumber("batch-poison");
		final String good2 = txNumber("batch-good2");

		final List<OrderConfirmation> placed = orderService.placeBatchIndependently(List.of(
				command(good1, "alice@example.com", new PlaceOrderCommand.Line("LAP-0001", 1)),
				command(poison, "alice@example.com", new PlaceOrderCommand.Line("NOPE-9999", 1)),
				command(good2, "bob@example.com", new PlaceOrderCommand.Line("PHN-0001", 1))));

		assertThat(placed).extracting(OrderConfirmation::orderNumber).containsExactly(good1, good2);
		assertThat(orderRows(good1)).isOne();
		assertThat(orderRows(poison)).isZero();
		assertThat(orderRows(good2)).isOne();
	}

	@Test
	void atomicBatchRollsBackEntirelyOnOneBadOrder() {
		final String good1 = txNumber("atomic-good1");
		final String poison = txNumber("atomic-poison");

		assertThatExceptionOfType(CatalogNotFoundException.class).isThrownBy(() -> orderService.placeBatchAtomically(List.of(
				command(good1, "alice@example.com", new PlaceOrderCommand.Line("LAP-0001", 1)),
				command(poison, "alice@example.com", new PlaceOrderCommand.Line("NOPE-9999", 1)))));

		assertThat(orderRows(good1)).isZero();
		assertThat(orderRows(poison)).isZero();
	}

	@Test
	void selfInvocationBypassesTheTransactionProxy() {
		assertThat(orderService.checkViaSelfInvocation()).isFalse();
		assertThat(orderService.checkViaInjectedProxy()).isTrue();
	}
}