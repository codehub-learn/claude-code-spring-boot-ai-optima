package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base type for every entity in the slice. Not an {@code @Entity} itself: {@code @MappedSuperclass} means the columns below are folded into
 * each concrete table ({@code category}, {@code product}, {@code customer}, {@code "order"}, {@code order_item}), all of which already
 * carry the {@code id} / {@code created_at} / {@code updated_at} / {@code version} quartet from their Flyway DDL.
 *
 * <ul>
 *   <li><b>{@code id}</b> — {@code GenerationType.IDENTITY} maps to Oracle {@code generated always as identity}. Hibernate cannot batch
 *       inserts for identity keys (it needs the generated value back per row); acceptable for this app.</li>
 *   <li><b>{@code version}</b> — {@code @Version} optimistic locking. Hibernate seeds it to 0 on insert and bumps it on every flush that
 *       dirties the row; a stale value on update throws {@code OptimisticLockException}.</li>
 *   <li><b>{@code createdAt} / {@code updatedAt}</b> — Spring Data JPA auditing via {@link AuditingEntityListener}. Only fires when
 *       {@code @EnableJpaAuditing} is present ({@code config.persistence.PersistenceConfig}). {@code createdAt} is {@code updatable =
 *       false}
 *       so a later flush never rewrites it. The type is {@link Instant}: Spring Data's auditing converter supports
 *       {@code LocalDateTime} / {@code Instant} / {@code Date} / {@code Long} but <b>not</b> {@code OffsetDateTime}. {@code @JdbcTypeCode}
 *       keeps the column mapped to Oracle {@code timestamp with time zone} (the type Flyway created in {@code V1}), storing the instant at
 *       UTC.</li>
 * </ul>
 *
 * <p>No {@code equals}/{@code hashCode} here — each subclass defines them against its own business key (CLAUDE.md), never the surrogate id.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class AbstractAuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private @Nullable Long id;

	@CreatedDate
	@JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private @Nullable Instant createdAt;

	@LastModifiedDate
	@JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
	@Column(name = "updated_at", nullable = false)
	private @Nullable Instant updatedAt;

	@Version
	@Column(nullable = false)
	private @Nullable Long version;

	/**
	 * {@code true} until the entity has been persisted (no generated id yet). Handy in {@code equals} and for guarding logic that only
	 * makes sense on a managed instance.
	 */
	boolean isNew() {
		return id == null;
	}
}