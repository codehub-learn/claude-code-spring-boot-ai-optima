package gr.codelearn.springbootai.training.support;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.oracle.OracleContainer;

/**
 * Base for the persistence <b>slice</b> tests - {@code @DataJpaTest} and {@code @JdbcTest}. The slice annotation itself stays on the
 * concrete class; this base carries the three things every slice here needs:
 *
 * <ul>
 *   <li>{@link AutoConfigureTestDatabase}{@code (replace = NONE)} - a slice otherwise swaps in an embedded database,
 *       and there is none on the classpath (the whole point is to run against Oracle).</li>
 *   <li>{@code @ActiveProfiles("test")} - turns on Boot-managed Flyway ({@code application-test.yml}). Both slices
 *       import {@code FlywayAutoConfiguration} ({@code @AutoConfigureJdbc} pulls
 *       {@code @AutoConfigureDataSourceInitialization}, which {@code spring-boot-flyway} contributes to), so the
 *       slice context migrates the schema + baseline seed exactly like the {@code @SpringBootTest} path.</li>
 *   <li>the shared {@link SharedOracleContainer} via {@link ServiceConnection} - one Oracle for the JVM, not one
 *       per slice style.</li>
 * </ul>
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractOraclePersistenceSliceTest {

	@ServiceConnection
	protected static final OracleContainer ORACLE = SharedOracleContainer.INSTANCE;
}