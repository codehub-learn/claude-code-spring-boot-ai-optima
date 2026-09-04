package gr.codelearn.springbootai.training.foundation.di;

/**
 * Reference note (unit 2.1): a constructor-injection cycle and how Spring Boot 4 reacts. Holds no beans on purpose; the example is
 * commented so the context still starts.
 *
 * <p>Uncommenting two mutually dependent components:
 *
 * <pre>{@code
 * @Component
 * class InvoiceService {
 *     InvoiceService(TaxService taxService) { ... }        // needs TaxService
 * }
 *
 * @Component
 * class TaxService {
 *     TaxService(InvoiceService invoiceService) { ... }    // needs InvoiceService
 * }
 * }</pre>
 *
 * <p>Since Spring Boot 2.6 circular references are rejected by default, so startup aborts with:
 *
 * <pre>
 * The dependencies of some of the beans in the application context form a cycle:
 *
 *    invoiceService defined in ...InvoiceService
 * ┌─────┐
 * |  taxService defined in ...TaxService
 * └─────┘
 * </pre>
 *
 * <p>Fixes, best first:
 * <ol>
 *   <li>break the cycle: move the shared behaviour into a third collaborator, or decouple the two with an
 *       {@code ApplicationEvent};</li>
 *   <li>inject one side lazily: {@code TaxService(@Lazy InvoiceService invoiceService)} — Spring injects a
 *       proxy and resolves the real bean on first call;</li>
 *   <li>last resort: {@code spring.main.allow-circular-references=true}, which only rescues setter/field
 *       injection; a constructor-to-constructor cycle still cannot be built.</li>
 * </ol>
 */
final class CircularDependencyExample {

	private CircularDependencyExample() {
	}
}