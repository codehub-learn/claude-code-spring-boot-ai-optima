package gr.codelearn.springbootai.persistencedemo.depot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import gr.codelearn.springbootai.persistencedemo.common.AbstractAuditableEntity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A Dromeas depot: one of the two central hubs or a local depot. Couriers work out of a depot, customers are served by one, and every
 * shipment has an origin and a destination depot.
 *
 * <p>No inverse collections ({@code couriers}, {@code shipments}) on purpose: a depot's shipments run into the tens of thousands, so they
 * are read through repository queries, never by walking an association. Business key: {@code code} (no setter, it is immutable).
 */
@Getter
@Entity
@Table(name = "depot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Depot extends AbstractAuditableEntity {

	@Column(nullable = false, unique = true, length = 10)
	private String code;

	@Setter
	@Column(nullable = false, length = 120)
	private String name;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "depot_type", nullable = false, length = 10)
	private DepotType depotType;

	@Setter
	@Column(nullable = false, length = 120)
	private String city;

	@Setter
	@Column(nullable = false, length = 120)
	private String region;

	@Setter
	@Column(name = "address_line", nullable = false, length = 200)
	private String addressLine;

	@Setter
	@Column(name = "postal_code", nullable = false, length = 20)
	private String postalCode;

	@Column(name = "opened_on", nullable = false)
	private LocalDate openedOn;

	Depot(final String code, final String name, final DepotType depotType, final String city, final String region,
		  final String addressLine, final String postalCode, final LocalDate openedOn) {
		this.code = code;
		this.name = name;
		this.depotType = depotType;
		this.city = city;
		this.region = region;
		this.addressLine = addressLine;
		this.postalCode = postalCode;
		this.openedOn = openedOn;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Depot other)) {
			return false;
		}
		return Objects.equals(code, other.getCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}

	@Override
	public String toString() {
		return "Depot{id=%d, code='%s', type=%s}".formatted(getId(), code, depotType);
	}
}