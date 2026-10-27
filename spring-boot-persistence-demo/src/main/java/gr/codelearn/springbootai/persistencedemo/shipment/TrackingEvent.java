package gr.codelearn.springbootai.persistencedemo.shipment;

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
import org.jspecify.annotations.Nullable;
import gr.codelearn.springbootai.persistencedemo.common.AbstractEntity;
import gr.codelearn.springbootai.persistencedemo.depot.Depot;

import java.time.OffsetDateTime;

/**
 * One scan in a {@link Shipment}'s history: where ({@code depot}), what ({@code status}) and when ({@code occurredAt}). An append-only
 * log, so it extends {@link AbstractEntity} (id and {@code created_at}) rather than {@code AbstractAuditableEntity}: no
 * {@code updated_at}, no {@code version}, no setters.
 *
 * <p>Created only through {@link Shipment#addEvent}, which keeps both sides of the association in sync. It has no business key, so
 * {@code equals}/{@code hashCode} stay on object identity.
 */
@Getter
@Entity
@Table(name = "tracking_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingEvent extends AbstractEntity {

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "depot_id", nullable = false)
	private Depot depot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ShipmentStatus status;

	@Column(name = "occurred_at", nullable = false)
	private OffsetDateTime occurredAt;

	@Column(length = 200)
	private @Nullable String note;

	TrackingEvent(final Shipment shipment, final Depot depot, final ShipmentStatus status, final OffsetDateTime occurredAt,
				  final @Nullable String note) {
		this.shipment = shipment;
		this.depot = depot;
		this.status = status;
		this.occurredAt = occurredAt;
		this.note = note;
	}

	@Override
	public String toString() {
		return "TrackingEvent{id=%d, status=%s, occurredAt=%s}".formatted(getId(), status, occurredAt);
	}
}