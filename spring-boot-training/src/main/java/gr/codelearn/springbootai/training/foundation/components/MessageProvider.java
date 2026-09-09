package gr.codelearn.springbootai.training.foundation.components;

/**
 * One interface, three implementations, three stereotypes ({@code @Component}, {@code @Service}, {@code @Repository}). Component scan finds
 * all three, and an injected {@code List<MessageProvider>} holds them all — the point of {@link StereotypeInventory}.
 */
interface MessageProvider {

	/**
	 * A short label for which bean answered, so tests can tell the implementations apart without real I/O.
	 */
	String stereotype();

	String message();
}