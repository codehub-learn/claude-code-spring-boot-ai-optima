package gr.codelearn.springbootai.persistencedemo.customer;

/**
 * Whether a {@link Customer} is a person or a company. Drives which identity columns are filled ({@code ck_customer_identity}).
 */
public enum CustomerType {
	PRIVATE,
	BUSINESS
}