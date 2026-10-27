package gr.codelearn.springbootai.persistencedemo.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * Base of every mutable entity: {@link AbstractEntity}'s {@code id} / {@code created_at} plus {@code updated_at} and {@code version}, the
 * trio every mutable table in {@code V1__create_delivery_schema.sql} carries.
 *
 * <ul>
 *   <li><b>{@code updatedAt}</b>: {@code @LastModifiedDate} auditing, refreshed on every flush that dirties the row.</li>
 *   <li><b>{@code version}</b>: {@code @Version} optimistic locking. Hibernate seeds it to 0 on insert and bumps it on every dirtying
 *       flush; a stale value on update throws {@code OptimisticLockException}.</li>
 * </ul>
 */
@Getter
@MappedSuperclass
public abstract class AbstractAuditableEntity extends AbstractEntity {

	@LastModifiedDate
	@JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
	@Column(name = "updated_at", nullable = false)
	private @Nullable Instant updatedAt;

	@Version
	@Column(nullable = false)
	private @Nullable Long version;
}