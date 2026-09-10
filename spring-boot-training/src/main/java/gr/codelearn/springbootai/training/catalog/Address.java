package gr.codelearn.springbootai.training.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

/**
 * A postal address as an {@code @Embeddable} value object. It has no identity and no lifecycle of its own: its columns live inside the
 * owning entity's table. It is a {@code record} because it is an immutable value (CLAUDE.md: "records for value objects"; Lombok is only
 * for entities). Hibernate 7 maps record embeddables for plain {@code @Embedded} fields.
 *
 * <p>Embedded twice: {@link Customer#getBillingAddress()} keeps the default column names below, and {@link Order} re-embeds it as the
 * shipping address with {@code @AttributeOverrides} that prefix every column with {@code ship_}.
 */
@Embeddable
record Address(
		@Column(name = "line1", nullable = false) String line1,
		@Column(name = "line2") @Nullable String line2,
		@Column(name = "city", nullable = false) String city,
		@Column(name = "postal_code", nullable = false) String postalCode,
		@Column(name = "country", nullable = false) String country) {
}