package gr.codelearn.springbootai.persistencedemo.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base of every entity: the identity primary key and the {@code created_at} audit column. {@code tracking_event} is an append-only log, so
 * it extends this class directly; every mutable table extends {@link AbstractAuditableEntity}, which adds {@code updated_at} and
 * {@code version}.
 *
 * <ul>
 *   <li><b>{@code id}</b>: {@code GenerationType.IDENTITY} maps to Oracle {@code generated always as identity}. Hibernate cannot batch
 *       identity inserts (it needs the generated value back per row).</li>
 *   <li><b>{@code createdAt}</b>: Spring Data auditing via {@link AuditingEntityListener}; it only fires when {@code @EnableJpaAuditing}
 *       is present ({@code config.persistence.PersistenceConfig}). It is {@code updatable = false}. The type is {@link Instant} because
 *       Spring Data's auditing converter does not support {@code OffsetDateTime}; {@code @JdbcTypeCode} keeps the column mapped to
 *       {@code timestamp with time zone}.</li>
 * </ul>
 *
 * <p>No {@code equals}/{@code hashCode} here: each subclass defines them against its own business key, never the surrogate id.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class AbstractEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private @Nullable Long id;

	@CreatedDate
	@JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private @Nullable Instant createdAt;

	/**
	 * {@code true} until the entity has been persisted (no generated id yet).
	 */
	boolean isNew() {
		return id == null;
	}
}