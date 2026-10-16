package gr.codelearn.springbootai.coredemo.common;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

/**
 * Showcase-only persistence stand-in: a {@link ConcurrentMap} plus an id sequence. {@link #update} is atomic per key, so concurrent
 * stock reservations cannot oversell.
 */
public abstract class InMemoryStore<T> {

	private final ConcurrentMap<Long, T> store = new ConcurrentHashMap<>();
	private final AtomicLong sequence = new AtomicLong();
	private final ToLongFunction<T> idOf;

	protected InMemoryStore(ToLongFunction<T> idOf) {
		this.idOf = idOf;
	}

	public long nextId() {
		return sequence.incrementAndGet();
	}

	public T save(T item) {
		store.put(idOf.applyAsLong(item), item);
		return item;
	}

	public Optional<T> findById(long id) {
		return Optional.ofNullable(store.get(id));
	}

	public List<T> findAll() {
		return List.copyOf(store.values());
	}

	/**
	 * Applies {@code change} to the stored item atomically; an exception thrown by it leaves the item untouched.
	 */
	public Optional<T> update(long id, UnaryOperator<T> change) {
		return Optional.ofNullable(store.computeIfPresent(id, (key, current) -> change.apply(current)));
	}
}
