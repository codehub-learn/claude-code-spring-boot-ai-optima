package gr.codelearn.springbootai.training.support;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * The single Oracle container for the whole test JVM.
 *
 * <p>Both persistence test styles reference this same instance: {@link AbstractOracleIntegrationTest} (full
 * {@code @SpringBootTest} contexts) and {@link AbstractOraclePersistenceSliceTest} ({@code @DataJpaTest} / {@code @JdbcTest}). Sharing one
 * field means one JVM starts Oracle once - not once per test style - even though the two base classes build different Spring contexts.
 *
 * <p>The container is given a fixed Docker name ({@code CONTAINER_NAME}) so it is easy to spot in {@code docker ps}
 * next to the {@code compose.yaml} service and to tail its log. A fixed name is not unique across processes, and {@code verify} runs two
 * JVMs (Surefire then Failsafe), so the static initialiser force-removes any leftover container with that name before starting a fresh one
 * - covering the JVM hand-off and a previous run that was killed before Ryuk could reap. {@code withReuse} is deliberately not used: it
 * only helps when a developer has globally opted in ({@code ~/.testcontainers.properties}), and without that opt-in it leaves the container
 * orphaned instead of Ryuk-reaped.
 *
 * <p>The image is {@code gvenzl/oracle-free:23-slim} (Oracle Database 23ai Free), matching {@code compose.yaml}.
 */
public final class SharedOracleContainer {

	private static final String CONTAINER_NAME = "training-oracle-it";

	public static final OracleContainer INSTANCE =
			new OracleContainer(DockerImageName.parse("gvenzl/oracle-free:23-slim"))
					.withLabel("gr.codelearn.module", "spring-boot-training")
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