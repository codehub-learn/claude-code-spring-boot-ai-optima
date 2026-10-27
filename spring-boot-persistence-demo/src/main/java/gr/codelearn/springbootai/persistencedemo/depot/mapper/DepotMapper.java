package gr.codelearn.springbootai.persistencedemo.depot.mapper;

import gr.codelearn.springbootai.persistencedemo.depot.Depot;
import gr.codelearn.springbootai.persistencedemo.depot.dto.DepotResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepotMapper {

	DepotResponse toResponse(Depot depot);
}