package gr.codelearn.springbootai.persistencedemo.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interface-based projection: Spring Data proxies it over the tuple of {@code InvoiceRepository#findSummariesByStatus}. The getter names
 * must match the JPQL aliases.
 */
public interface InvoiceSummary {

	String getInvoiceNumber();

	String getCustomerCode();

	LocalDate getDueOn();

	BigDecimal getTotalAmount();
}