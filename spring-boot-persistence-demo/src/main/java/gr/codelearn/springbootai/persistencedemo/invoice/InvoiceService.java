package gr.codelearn.springbootai.persistencedemo.invoice;

import gr.codelearn.springbootai.persistencedemo.common.ConflictException;
import gr.codelearn.springbootai.persistencedemo.common.NotFoundException;
import gr.codelearn.springbootai.persistencedemo.invoice.dto.InvoiceResponse;
import gr.codelearn.springbootai.persistencedemo.invoice.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use cases for {@link Invoice}. Invoices are generated in bulk by the monthly billing run (a later step), so this service reads them
 * and records payment.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class InvoiceService {

	private final InvoiceRepository invoiceRepository;
	private final InvoiceMapper invoiceMapper;

	Page<InvoiceResponse> findByCustomer(final String customerCode, final Pageable pageable) {
		return invoiceRepository.findByCustomerCustomerCode(customerCode, pageable).map(invoiceMapper::toResponse);
	}

	InvoiceResponse getByNumber(final String invoiceNumber) {
		return invoiceMapper.toResponse(requireByNumber(invoiceNumber));
	}

	@Transactional
	InvoiceResponse markPaid(final String invoiceNumber, final LocalDate paymentDate) {
		final Invoice invoice = requireByNumber(invoiceNumber);
		if (invoice.getStatus() == InvoiceStatus.PAID) {
			throw new ConflictException("Invoice '%s' is already paid".formatted(invoiceNumber));
		}
		invoice.markPaid(paymentDate);
		return invoiceMapper.toResponse(invoice);
	}

	Invoice requireByNumber(final String invoiceNumber) {
		return invoiceRepository.findByInvoiceNumber(invoiceNumber).orElseThrow(() -> new NotFoundException("Invoice", invoiceNumber));
	}
}