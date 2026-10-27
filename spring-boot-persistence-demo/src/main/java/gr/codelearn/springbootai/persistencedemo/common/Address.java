package gr.codelearn.springbootai.persistencedemo.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A street address as a value object. Embedded in {@code Customer} (columns {@code line1}, {@code city}, {@code postal_code}) and,
 * re-mapped with {@code @AttributeOverrides}, in {@code Shipment} (columns {@code recipient_line1}, {@code recipient_city},
 * {@code recipient_postal_code}).
 */
@Embeddable
public record Address(
		@Column(nullable = false, length = 200) String line1,
		@Column(nullable = false, length = 120) String city,
		@Column(name = "postal_code", nullable = false, length = 20) String postalCode) {
}