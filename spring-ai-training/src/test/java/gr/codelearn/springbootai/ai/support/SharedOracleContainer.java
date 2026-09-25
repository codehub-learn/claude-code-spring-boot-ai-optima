package gr.codelearn.springbootai.ai.support;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Unit 9.2: the single Oracle container for the whole test JVM (same pattern as spring-boot-training's). Every full-context test shares it
 * through {@link AbstractOracleIntegrationTest}, so a JVM starts Oracle once even though the tests build different contexts (each
 * {@code *ApiIT} has its own stub {@code ChatModel}).
 *
 * <p>A fixed Docker name makes it easy to spot in {@code docker ps}. It is not unique across processes and {@code verify} runs two JVMs
 * (Surefire, then Failsafe), so a leftover container with that name is force-removed before the new one starts. The name differs from
 * spring-boot-training's, so a reactor build never removes the other module's container.
 */
public final class SharedOracleContainer {

	private static final String CONTAINER_NAME = "ai-training-oracle-it";

	public static final OracleContainer INSTANCE =
			new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"))
					.withLabel("gr.codelearn.module", "spring-ai-training")
					.withCreateContainerCmdModifier(cmd -> cmd.withName(CONTAINER_NAME));

	static {
		removeStaleContainer();
		INSTANCE.start();
	}

	private static void removeStaleContainer() {
		final var docker = DockerClientFactory.instance().client();
		docker.listContainersCmd()
			  .withShowAll(true)
			  .withNameFilter(List.of(CONTAINER_NAME))
			  .exec()
			  .forEach(container -> docker.removeContainerCmd(container.getId()).withForce(true).exec());
	}

	private SharedOracleContainer() {
	}
}