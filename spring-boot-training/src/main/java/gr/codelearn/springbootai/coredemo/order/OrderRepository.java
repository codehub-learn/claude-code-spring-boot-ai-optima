package gr.codelearn.springbootai.coredemo.order;

import gr.codelearn.springbootai.coredemo.common.InMemoryStore;
import org.springframework.stereotype.Repository;

@Repository
class OrderRepository extends InMemoryStore<Order> {

	OrderRepository() {
		super(Order::id);
	}
}
