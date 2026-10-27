package gr.codelearn.springbootai.persistencedemo.customer.dto;

public record BusinessCustomerRequest(String customerCode, String companyName, String vatNumber, String email, String phone,
                                      String depotCode,
									  String line1, String city, String postalCode) {
}
