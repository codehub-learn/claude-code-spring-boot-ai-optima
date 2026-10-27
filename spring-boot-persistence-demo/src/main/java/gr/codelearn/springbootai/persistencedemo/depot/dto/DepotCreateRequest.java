package gr.codelearn.springbootai.persistencedemo.depot.dto;

import gr.codelearn.springbootai.persistencedemo.depot.DepotType;

import java.time.LocalDate;

public record DepotCreateRequest(String code, String name, DepotType depotType, String city, String region, String addressLine,
								 String postalCode, LocalDate openedOn) {
}
