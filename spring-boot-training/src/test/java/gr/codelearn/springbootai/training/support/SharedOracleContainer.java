package gr.codelearn.springbootai.training.support;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The single Oracle container for the whole test JVM.
 *
 * <p>Both persistence test styles reference this same instance: {@link AbstractOracleIntegrationTest} (full
 * {@code @SpringBootTest} contexts) and {@link AbstractOraclePersistenceSliceTest} ({@code @DataJpaTest} / {@code @JdbcTest}). Sharing one
 * field means {@code verify} starts Oracle once - not once per test style - even though the two base classes build different Spring
 * contexts.
 *
 * <p>Started in a static initialiser and never stopped explicitly; Testcontainers' Ryuk sidecar reaps it when the
 * JVM exits. The image is {@code gvenzl/oracle-free:23-slim} (Oracle Database 23ai Free), matching {@code compose.yaml}.
 */
public final class SharedOracleContainer {

	public static final OracleContainer INSTANCE =
			new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"));

	static {
		INSTANCE.start();
	}

	private SharedOracleContainer() {
	}
}