package gr.codelearn.springbootai.training.persistence;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * U3.1: proves the persistence stack agrees end to end - the Testcontainers Oracle datasource is wired, Flyway ran {@code V1}, and
 * Hibernate's {@code ddl-auto=validate} accepted the resulting schema (the context would not have started otherwise). Flyway runs here
 * because the {@code test} profile re-enables Boot's Flyway ({@code application-test.yml}); at normal app boot it is off and migrations go
 * through {@code ./mvnw flyway:migrate}. {@code *IT} so Failsafe runs it in the integration-test phase; needs a Docker daemon.
 */
class DatasourceMigrationIT extends AbstractOracleIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void flywayRecordedV1AsSuccessful() {
		// Flyway creates its history table with quoted (case-sensitive, lower-case) identifiers on Oracle;
		// "success" is a NUMBER(1), so read it as an int.
		Integer success = jdbcClient.sql("select \"success\" from \"flyway_schema_history\" where \"version\" = '1'")
									.query(Integer.class)
									.single();

		assertThat(success).isEqualTo(1);
	}

	@Test
	void catalogTablesExist() {
		// Unquoted DDL identifiers are folded to upper case in the Oracle data dictionary.
		var tables = jdbcClient.sql("select table_name from user_tables")
							   .query(String.class)
							   .list();

		assertThat(tables).contains("CATEGORY", "PRODUCT");
	}

	@Test
	void productHasForeignKeyToCategory() {
		Long count = jdbcClient.sql("""
									select count(*) from user_constraints
									where constraint_name = 'FK_PRODUCT_CATEGORY'
									and constraint_type = 'R'
									""")
							   .query(Long.class)
							   .single();

		assertThat(count).isEqualTo(1L);
	}
}