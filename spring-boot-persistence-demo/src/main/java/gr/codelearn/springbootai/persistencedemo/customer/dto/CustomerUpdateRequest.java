package gr.codelearn.springbootai.persistencedemo.customer.dto;

import gr.codelearn.springbootai.persistencedemo.customer.ContractTier;

public record CustomerUpdateRequest(String email, String phone, ContractTier contractTier, String line1, String city, String postalCode) {
}