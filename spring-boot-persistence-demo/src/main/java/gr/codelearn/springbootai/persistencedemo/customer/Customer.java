package gr.codelearn.springbootai.persistencedemo.customer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import gr.codelearn.springbootai.persistencedemo.common.AbstractAuditableEntity;
import gr.codelearn.springbootai.persistencedemo.common.Address;
import gr.codelearn.springbootai.persistencedemo.depot.Depot;

import java.util.Objects;

/**
 * Someone who sends shipments: a private person (first and last name) or a business (company name and 9-digit Greek VAT number). The
 * static factories {@link #privateCustomer} and {@link #business} build a valid instance of each kind, matching the
 * {@code ck_customer_identity} check constraint, so callers cannot assemble an inconsistent mix.
 *
 * <p>The address is an {@code @Embedded} {@link Address}: its columns sit directly on the {@code customer} table. {@code depot} is the
 * depot that serves the customer's pickups. Business key: {@code customerCode} (no setter).
 */
@Getter
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends AbstractAuditableEntity {

	@Column(name = "customer_code", nullable = false, unique = true, length = 20)
	private String customerCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "customer_type", nullable = false, length = 10)
	private CustomerType customerType;

	@Setter
	@Column(name = "first_name", length = 100)
	private @Nullable String firstName;

	@Setter
	@Column(name = "last_name", length = 100)
	private @Nullable String lastName;

	@Setter
	@Column(name = "company_name", length = 200)
	private @Nullable String companyName;

	@Setter
	@Column(name = "vat_number", unique = true, length = 9)
	private @Nullable String vatNumber;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "contract_tier", nullable = false, length = 10)
	private ContractTier contractTier = ContractTier.STANDARD;

	@Setter
	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Setter
	@Column(nullable = false, length = 30)
	private String phone;

	@Setter
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "depot_id", nullable = false)
	private Depot depot;

	@Setter
	@Embedded
	private Address address;

	@Setter
	@Column(nullable = false, length = 2)
	private String country = "GR";

	private Customer(final String customerCode, final CustomerType customerType, final String email, final String phone,
					 final Depot depot, final Address address) {
		this.customerCode = customerCode;
		this.customerType = customerType;
		this.email = email;
		this.phone = phone;
		this.depot = depot;
		this.address = address;
	}

	static Customer privateCustomer(final String customerCode, final String firstName, final String lastName, final String email,
									final String phone, final Depot depot, final Address address) {
		final Customer customer = new Customer(customerCode, CustomerType.PRIVATE, email, phone, depot, address);
		customer.firstName = firstName;
		customer.lastName = lastName;
		return customer;
	}

	static Customer business(final String customerCode, final String companyName, final String vatNumber, final String email,
							 final String phone, final Depot depot, final Address address) {
		final Customer customer = new Customer(customerCode, CustomerType.BUSINESS, email, phone, depot, address);
		customer.companyName = companyName;
		customer.vatNumber = vatNumber;
		return customer;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Customer other)) {
			return false;
		}
		return Objects.equals(customerCode, other.getCustomerCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(customerCode);
	}

	@Override
	public String toString() {
		return "Customer{id=%d, customerCode='%s', type=%s}".formatted(getId(), customerCode, customerType);
	}
}
