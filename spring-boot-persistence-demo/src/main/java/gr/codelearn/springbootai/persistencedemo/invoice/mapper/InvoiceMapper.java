package gr.codelearn.springbootai.persistencedemo.invoice.mapper;

import gr.codelearn.springbootai.persistencedemo.invoice.Invoice;
import gr.codelearn.springbootai.persistencedemo.invoice.dto.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

	@Mapping(target = "customerCode", source = "customer.customerCode")
	InvoiceResponse toResponse(Invoice invoice);
}