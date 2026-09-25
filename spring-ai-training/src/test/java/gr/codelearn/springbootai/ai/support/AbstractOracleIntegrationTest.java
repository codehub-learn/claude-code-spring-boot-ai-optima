package gr.codelearn.springbootai.ai.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.oracle.OracleContainer;

/**
 * Unit 9.2: base class of every {@code @SpringBootTest} in the module. Chat memory put a {@code DataSource} on the classpath, so a full
 * context no longer starts without a database, even for tests that never touch memory.
 *
 * <p>{@link ServiceConnection} contributes {@code spring.datasource.*} from the container; the {@code test} profile turns Boot's Flyway
 * on,
 * so the schema ({@code db/migration}) is built on context start.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOracleIntegrationTest {

	@ServiceConnection
	protected static final OracleContainer ORACLE = SharedOracleContainer.INSTANCE;
}