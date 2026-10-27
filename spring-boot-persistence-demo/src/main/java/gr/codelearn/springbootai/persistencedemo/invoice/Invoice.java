package gr.codelearn.springbootai.persistencedemo.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import gr.codelearn.springbootai.persistencedemo.common.AbstractAuditableEntity;
import gr.codelearn.springbootai.persistencedemo.customer.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The monthly invoice of a business {@link Customer}: it covers that customer's delivered shipments of one period (the shipments point
 * back through {@code Shipment#getInvoice()}). One invoice per customer and period, enforced by {@code uq_invoice_customer_period}.
 *
 * <p>No {@code shipments} collection: an invoice can cover hundreds of shipments and is read-mostly, so its lines are queried, not
 * navigated. {@code paidOn} is set only for {@code PAID} invoices ({@code ck_invoice_paid_on}). Business key: {@code invoiceNumber}.
 */
@Getter
@Entity
@Table(name = "invoice", uniqueConstraints = @UniqueConstraint(name = "uq_invoice_customer_period",
		columnNames = {"customer_id", "period_start"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends AbstractAuditableEntity {

	@Column(name = "invoice_number", nullable = false, unique = true, length = 30)
	private String invoiceNumber;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", nullable = false)
	private LocalDate periodEnd;

	@Column(name = "issued_on", nullable = false)
	private LocalDate issuedOn;

	@Setter
	@Column(name = "due_on", nullable = false)
	private LocalDate dueOn;

	@Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal netAmount;

	@Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal vatAmount;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private InvoiceStatus status = InvoiceStatus.ISSUED;

	@Setter
	@Column(name = "paid_on")
	private @Nullable LocalDate paidOn;

	Invoice(final String invoiceNumber, final Customer customer, final LocalDate periodStart, final LocalDate periodEnd,
			final LocalDate issuedOn, final LocalDate dueOn, final BigDecimal netAmount, final BigDecimal vatAmount) {
		this.invoiceNumber = invoiceNumber;
		this.customer = customer;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.issuedOn = issuedOn;
		this.dueOn = dueOn;
		this.netAmount = netAmount;
		this.vatAmount = vatAmount;
		this.totalAmount = netAmount.add(vatAmount);
	}

	/**
	 * Marks the invoice paid on the given date, keeping {@code status} and {@code paidOn} consistent with {@code ck_invoice_paid_on}.
	 */
	void markPaid(final LocalDate paymentDate) {
		this.status = InvoiceStatus.PAID;
		this.paidOn = paymentDate;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Invoice other)) {
			return false;
		}
		return Objects.equals(invoiceNumber, other.getInvoiceNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(invoiceNumber);
	}

	@Override
	public String toString() {
		return "Invoice{id=%d, invoiceNumber='%s', status=%s}".formatted(getId(), invoiceNumber, status);
	}
}