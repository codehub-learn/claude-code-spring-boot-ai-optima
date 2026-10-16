package gr.codelearn.springbootai.coredemo.customer;

public record Address(String street, String city, String postalCode, String country, AddressType type) {
}
