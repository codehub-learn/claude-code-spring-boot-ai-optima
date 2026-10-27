package gr.codelearn.springbootai.persistencedemo.courier.mapper;

import gr.codelearn.springbootai.persistencedemo.courier.Courier;
import gr.codelearn.springbootai.persistencedemo.courier.dto.CourierResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourierMapper {

	@Mapping(target = "depotCode", source = "depot.code")
	CourierResponse toResponse(Courier courier);
}