package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Whether a {@link Customer} is a person or a company. Drives which identity columns are filled ({@code ck_customer_identity}).
 */
enum CustomerType {
	PRIVATE,
	BUSINESS
}