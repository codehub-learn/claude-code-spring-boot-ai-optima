package gr.codelearn.springbootai.persistencedemo.customer.mapper;

import gr.codelearn.springbootai.persistencedemo.customer.Customer;
import gr.codelearn.springbootai.persistencedemo.customer.dto.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

	@Mapping(target = "depotCode", source = "depot.code")
	@Mapping(target = "line1", source = "address.line1")
	@Mapping(target = "city", source = "address.city")
	@Mapping(target = "postalCode", source = "address.postalCode")
	CustomerResponse toResponse(Customer customer);
}