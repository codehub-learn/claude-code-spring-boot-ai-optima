package gr.codelearn.springbootai.persistencedemo.shipment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import gr.codelearn.springbootai.persistencedemo.common.AbstractAuditableEntity;
import gr.codelearn.springbootai.persistencedemo.common.Address;
import gr.codelearn.springbootai.persistencedemo.courier.Courier;
import gr.codelearn.springbootai.persistencedemo.customer.Customer;
import gr.codelearn.springbootai.persistencedemo.depot.Depot;
import gr.codelearn.springbootai.persistencedemo.invoice.Invoice;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A parcel and the aggregate root of {@code shipment 1..N tracking_event}.
 *
 * <p>Associations (all {@code LAZY}):
 * <ul>
 *   <li>{@code customer} (the sender), {@code originDepot} and {@code destinationDepot}: required {@code @ManyToOne};</li>
 *   <li>{@code courier}: optional, null until the shipment goes out for delivery;</li>
 *   <li>{@code invoice}: optional, null until the shipment is billed;</li>
 *   <li>{@code events}: {@code @OneToMany(mappedBy = "shipment")} with {@code cascade = ALL} and {@code orphanRemoval}, ordered by
 *       {@code occurredAt}. A shipment has a handful of scans, so navigating the collection is cheap. {@link TrackingEvent} owns the
 *       foreign key.</li>
 * </ul>
 *
 * <p>The recipient's address re-embeds {@link Address} with {@code @AttributeOverrides} onto the {@code recipient_*} columns.
 * {@code pickedUpAt}, {@code deliveredAt} and {@code attempts} are denormalised from the events for cheap reads. Business key:
 * {@code trackingNumber} (no setter).
 */
@Getter
@Entity
@Table(name = "shipment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends AbstractAuditableEntity {

	@Column(name = "tracking_number", nullable = false, unique = true, length = 12)
	private String trackingNumber;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "origin_depot_id", nullable = false)
	private Depot originDepot;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_depot_id", nullable = false)
	private Depot destinationDepot;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "courier_id")
	private @Nullable Courier courier;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice_id")
	private @Nullable Invoice invoice;

	@Enumerated(EnumType.STRING)
	@Column(name = "service_level", nullable = false, length = 10)
	private ServiceLevel serviceLevel;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ShipmentStatus status = ShipmentStatus.CREATED;

	@Column(name = "weight_kg", nullable = false, precision = 7, scale = 2)
	private BigDecimal weightKg;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "recipient_name", nullable = false, length = 200)
	private String recipientName;

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "line1", column = @Column(name = "recipient_line1", nullable = false, length = 200)),
			@AttributeOverride(name = "city", column = @Column(name = "recipient_city", nullable = false, length = 120)),
			@AttributeOverride(name = "postalCode", column = @Column(name = "recipient_postal_code", nullable = false, length = 20))
	})
	private Address recipientAddress;

	@Setter
	@Column(name = "picked_up_at")
	private @Nullable OffsetDateTime pickedUpAt;

	@Setter
	@Column(name = "promised_by", nullable = false)
	private OffsetDateTime promisedBy;

	@Setter
	@Column(name = "delivered_at")
	private @Nullable OffsetDateTime deliveredAt;

	@Setter
	@Column(nullable = false)
	private int attempts;

	@OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("occurredAt")
	private final List<TrackingEvent> events = new ArrayList<>();

	Shipment(final String trackingNumber, final Customer customer, final Depot originDepot, final Depot destinationDepot,
			 final ServiceLevel serviceLevel, final BigDecimal weightKg, final BigDecimal price, final String recipientName,
			 final Address recipientAddress, final OffsetDateTime promisedBy) {
		this.trackingNumber = trackingNumber;
		this.customer = customer;
		this.originDepot = originDepot;
		this.destinationDepot = destinationDepot;
		this.serviceLevel = serviceLevel;
		this.weightKg = weightKg;
		this.price = price;
		this.recipientName = recipientName;
		this.recipientAddress = recipientAddress;
		this.promisedBy = promisedBy;
	}

	/**
	 * Read-only view of the scan history, oldest first. Mutate through {@link #addEvent} so both sides of the association stay consistent.
	 */
	List<TrackingEvent> getEvents() {
		return Collections.unmodifiableList(events);
	}

	/**
	 * Records a scan: builds a {@link TrackingEvent} bound to this shipment, adds it and moves the shipment to the event's status.
	 */
	TrackingEvent addEvent(final ShipmentStatus eventStatus, final Depot depot, final OffsetDateTime occurredAt,
						   final @Nullable String note) {
		final TrackingEvent event = new TrackingEvent(this, depot, eventStatus, occurredAt, note);
		events.add(event);
		this.status = eventStatus;
		return event;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Shipment other)) {
			return false;
		}
		return Objects.equals(trackingNumber, other.getTrackingNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(trackingNumber);
	}

	@Override
	public String toString() {
		return "Shipment{id=%d, trackingNumber='%s', status=%s}".formatted(getId(), trackingNumber, status);
	}
}
