package gr.codelearn.springbootai.coredemo.payment;

import gr.codelearn.springbootai.coredemo.common.InMemoryStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class PaymentRepository extends InMemoryStore<Payment> {

	PaymentRepository() {
		super(Payment::id);
	}

	Optional<Payment> findByOrderId(long orderId) {
		return findAll().stream().filter(payment -> payment.orderId() == orderId).findFirst();
	}
}
