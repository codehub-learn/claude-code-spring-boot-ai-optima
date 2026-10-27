package gr.codelearn.springbootai.persistencedemo.customer.dto;

public record PrivateCustomerRequest(String customerCode, String firstName, String lastName, String email, String phone, String depotCode,
									 String line1, String city, String postalCode) {
}
