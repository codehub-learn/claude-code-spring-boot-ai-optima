package gr.codelearn.springbootai.training.foundation.components;

import gr.codelearn.springbootai.training.support.AbstractOracleIntegrationTest;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit 2.3: {@code @Component}, {@code @Service} and {@code @Repository} are one scan. The two specializations are meta-annotated
 * {@code @Component}, which is why {@link StereotypeInventory} receives all three in a single injected {@code List<MessageProvider>}.
 */
class ComponentStereotypeTest extends AbstractOracleIntegrationTest {

	@Autowired
	private StereotypeInventory inventory;

	@Test
	void componentScanFindsEveryStereotype() {
		assertThat(inventory.stereotypesByType()).containsOnly(MapEntry.entry("StaticMessageComponent", "component"),
															   MapEntry.entry("GreetingService", "service"),
															   MapEntry.entry("MessageRepositoryStub", "repository"));
	}

	@Test
	void everyStereotypeIsAlsoAComponent() {
		assertThat(inventory.allBeansAreComponents()).isTrue();
		assertThat(AnnotatedElementUtils.hasAnnotation(Service.class, Component.class)).isTrue();
		assertThat(AnnotatedElementUtils.hasAnnotation(Repository.class, Component.class)).isTrue();
	}

	@Test
	void everyProviderContributesAMessage() {
		assertThat(inventory.messages()).hasSize(3);
	}
}