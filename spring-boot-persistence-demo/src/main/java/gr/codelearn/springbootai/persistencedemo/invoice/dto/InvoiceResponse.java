package gr.codelearn.springbootai.persistencedemo.invoice.dto;

import gr.codelearn.springbootai.persistencedemo.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record InvoiceResponse(String invoiceNumber, String customerCode, LocalDate periodStart, LocalDate periodEnd, LocalDate issuedOn,
							  LocalDate dueOn, BigDecimal netAmount, BigDecimal vatAmount, BigDecimal totalAmount, InvoiceStatus status,
							  @Nullable LocalDate paidOn) {
}
