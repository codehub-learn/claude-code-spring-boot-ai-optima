package gr.codelearn.springbootai.training.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every persistence-touching integration test (and, since U3.1 puts a datasource on the classpath, every
 * {@code @SpringBootTest} in the module - a full context now needs a real database to start).
 *
 * <p>Singleton-container pattern: the container is a {@code static} field started once in a static initialiser and never stopped
 * explicitly. Testcontainers' Ryuk sidecar reaps it when the JVM exits. One container serves the whole {@code verify} run, and Spring's
 * context cache means the shared {@code test}-profile context is built once too.
 *
 * <p>{@link ServiceConnection} on the started container contributes {@code spring.datasource.*} with no {@code @DynamicPropertySource}
 * boilerplate. The image is {@code gvenzl/oracle-free:23-slim} (matching {@code compose.yaml}) - Oracle Database 23ai Free; the container
 * connects to the {@code FREEPDB1} pluggable database as its own throwaway app user.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOracleIntegrationTest {

	@ServiceConnection
	protected static final OracleContainer ORACLE =
			new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"));

	static {
		ORACLE.start();
	}
}