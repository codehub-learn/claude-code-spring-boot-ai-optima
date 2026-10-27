package gr.codelearn.springbootai.persistencedemo.courier.dto;

import gr.codelearn.springbootai.persistencedemo.courier.VehicleType;

import java.time.LocalDate;

public record CourierCreateRequest(String employeeCode, String depotCode, String firstName, String lastName, String phone,
                                   VehicleType vehicleType,
								   int dailyCapacity, LocalDate hiredOn) {
}
