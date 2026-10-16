package gr.codelearn.springbootai.coredemo.customer;

import gr.codelearn.springbootai.coredemo.common.InMemoryStore;
import org.springframework.stereotype.Repository;

@Repository
class CustomerRepository extends InMemoryStore<Customer> {

	CustomerRepository() {
		super(Customer::id);
	}
}
