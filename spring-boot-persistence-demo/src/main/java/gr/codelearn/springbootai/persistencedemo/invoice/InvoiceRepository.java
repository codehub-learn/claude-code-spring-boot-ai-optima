package gr.codelearn.springbootai.persistencedemo.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

	Page<Invoice> findByCustomerCustomerCode(String customerCode, Pageable pageable);
}