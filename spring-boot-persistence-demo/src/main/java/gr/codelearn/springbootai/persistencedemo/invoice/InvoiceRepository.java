package gr.codelearn.springbootai.persistencedemo.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

	Page<Invoice> findByCustomerCustomerCode(String customerCode, Pageable pageable);

	/**
	 * Issued invoices whose due date has passed, with the customer fetched for the response. The count query is explicit because Spring
	 * Data cannot derive one from a {@code join fetch}.
	 */
	@Query(value = "select i from Invoice i join fetch i.customer where i.status = :status and i.dueOn < :today",
		   countQuery = "select count(i) from Invoice i where i.status = :status and i.dueOn < :today")
	Page<Invoice> findPastDue(@Param("status") InvoiceStatus status, @Param("today") LocalDate today, Pageable pageable);

	@Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.customer.customerCode = :customerCode and i.status <> :paid")
	BigDecimal sumOutstandingByCustomerCode(@Param("customerCode") String customerCode, @Param("paid") InvoiceStatus paid);
}