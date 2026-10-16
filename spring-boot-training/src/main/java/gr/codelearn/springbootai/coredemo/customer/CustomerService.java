package gr.codelearn.springbootai.coredemo.customer;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

	private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

	private final CustomerRepository customers;

	CustomerService(CustomerRepository customers) {
		this.customers = customers;
	}

	public Customer register(String email, String firstName, String lastName, @Nullable String phone) {
		var customer = customers.save(new Customer(customers.nextId(), email, firstName, lastName, phone, List.of()));
		log.info("Registered customer {}", customer.id());
		return customer;
	}

	public Customer findById(long id) {
		return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
	}

	public Customer addAddress(long customerId, Address address) {
		return customers.update(customerId, customer -> customer.withAddress(address))
				.orElseThrow(() -> new CustomerNotFoundException(customerId));
	}
}
