package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * A person who places {@link Order}s. Holds an {@link Address} as an {@code @Embedded} value object — the address columns ({@code line1},
 * {@code city}, {@code postal_code}, …) sit directly on the {@code customer} table, no join.
 *
 * <p>Business key: {@code email}. No setter — see {@link Category} for why the key is immutable.
 */
@Getter
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Customer extends AbstractAuditableEntity {

	@Setter
	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Setter
	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(nullable = false, unique = true)
	private String email;

	@Setter
	@Embedded
	private Address billingAddress;

	Customer(final String email, final String firstName, final String lastName, final Address billingAddress) {
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;
		this.billingAddress = billingAddress;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Customer other)) {
			return false;
		}
		return Objects.equals(email, other.getEmail());
	}

	@Override
	public int hashCode() {
		return Objects.hash(email);
	}

	@Override
	public String toString() {
		return "Customer{id=%d, email='%s'}".formatted(getId(), email);
	}
}