package gr.codelearn.springbootai.persistencedemo.customer.dto;

import gr.codelearn.springbootai.persistencedemo.customer.ContractTier;
import gr.codelearn.springbootai.persistencedemo.customer.CustomerType;
import org.jspecify.annotations.Nullable;

public record CustomerResponse(String customerCode, CustomerType customerType, @Nullable String firstName, @Nullable String lastName,
							   @Nullable String companyName, @Nullable String vatNumber, ContractTier contractTier, String email,
							   String phone, String depotCode, String line1, String city, String postalCode, String country) {
}
