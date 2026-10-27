package gr.codelearn.springbootai.persistencedemo.courier.dto;

import gr.codelearn.springbootai.persistencedemo.courier.VehicleType;

public record CourierUpdateRequest(String phone, VehicleType vehicleType, int dailyCapacity, boolean active) {
}