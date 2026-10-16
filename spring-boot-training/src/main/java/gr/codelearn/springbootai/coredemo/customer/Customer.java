package gr.codelearn.springbootai.coredemo.customer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record Customer(long id, String email, String firstName, String lastName, @Nullable String phone, List<Address> addresses) {

	public Customer {
		addresses = List.copyOf(addresses);
	}

	public Customer withAddress(Address address) {
		var updated = new ArrayList<>(addresses);
		updated.add(address);
		return new Customer(id, email, firstName, lastName, phone, updated);
	}
}
