package gr.codelearn.springbootai.ai.mcp.support;

import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The single Oracle container of the test JVM: the integration tests build different contexts (tool style, sampling) but must not start one
 * database each.
 */
public final class SharedOracleContainer {

	public static final OracleContainer INSTANCE = new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"));

	static {
		INSTANCE.start();
	}

	private SharedOracleContainer() {
	}
}