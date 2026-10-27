package gr.codelearn.springbootai.persistencedemo.depot.dto;

public record DepotUpdateRequest(String name, String city, String region, String addressLine, String postalCode) {
}