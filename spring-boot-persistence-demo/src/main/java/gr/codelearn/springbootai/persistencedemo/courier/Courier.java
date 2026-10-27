package gr.codelearn.springbootai.persistencedemo.courier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import gr.codelearn.springbootai.persistencedemo.common.AbstractAuditableEntity;
import gr.codelearn.springbootai.persistencedemo.depot.Depot;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A courier who delivers shipments out of one {@link Depot}. {@code dailyCapacity} is the number of stops the courier can take per day;
 * dispatching a shipment must respect it, and the inherited {@code @Version} makes two dispatchers racing for the last free slot fail
 * loudly with an optimistic-lock conflict instead of overbooking.
 *
 * <p>{@code active} is stored as {@code number(1)}: {@code @JdbcTypeCode(TINYINT)} keeps the Java {@code boolean} on a numeric column
 * rather than Oracle 23ai's native {@code BOOLEAN}. Business key: {@code employeeCode} (no setter).
 */
@Getter
@Entity
@Table(name = "courier")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier extends AbstractAuditableEntity {

	@Setter
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "depot_id", nullable = false)
	private Depot depot;

	@Column(name = "employee_code", nullable = false, unique = true, length = 20)
	private String employeeCode;

	@Setter
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Setter
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Setter
	@Column(nullable = false, length = 30)
	private String phone;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "vehicle_type", nullable = false, length = 20)
	private VehicleType vehicleType;

	@Setter
	@Column(name = "daily_capacity", nullable = false)
	private int dailyCapacity;

	@Setter
	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "hired_on", nullable = false)
	private LocalDate hiredOn;

	Courier(final Depot depot, final String employeeCode, final String firstName, final String lastName, final String phone,
			final VehicleType vehicleType, final int dailyCapacity, final LocalDate hiredOn) {
		this.depot = depot;
		this.employeeCode = employeeCode;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.vehicleType = vehicleType;
		this.dailyCapacity = dailyCapacity;
		this.hiredOn = hiredOn;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Courier other)) {
			return false;
		}
		return Objects.equals(employeeCode, other.getEmployeeCode());
	}

	@Override
	public int hashCode() {
		return Objects.hash(employeeCode);
	}

	@Override
	public String toString() {
		return "Courier{id=%d, employeeCode='%s', vehicle=%s}".formatted(getId(), employeeCode, vehicleType);
	}
}