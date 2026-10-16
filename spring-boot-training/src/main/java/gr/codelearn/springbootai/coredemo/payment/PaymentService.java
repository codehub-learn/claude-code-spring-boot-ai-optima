package gr.codelearn.springbootai.coredemo.payment;

import gr.codelearn.springbootai.coredemo.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository payments;
	private final OrderService orderService;
	private final Clock clock;

	PaymentService(PaymentRepository payments, OrderService orderService, Clock clock) {
		this.payments = payments;
		this.orderService = orderService;
		this.clock = clock;
	}

	/** Simulated gateway: always succeeds. The order transition fails first if the order is not payable, so no payment is recorded. */
	public Payment pay(long orderId, PaymentMethod method) {
		var order = orderService.markPaid(orderId);
		var payment = payments.save(
				new Payment(payments.nextId(), orderId, order.totalAmount(), method, PaymentStatus.COMPLETED, clock.instant()));
		log.info("Payment {} completed for order {}", payment.id(), order.orderNumber());
		return payment;
	}

	public Payment findByOrderId(long orderId) {
		return payments.findByOrderId(orderId).orElseThrow(() -> new PaymentNotFoundException(orderId));
	}
}
