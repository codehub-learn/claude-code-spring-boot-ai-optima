package gr.codelearn.springbootai.training.foundation.beans;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plain object used to show singleton vs {@code prototype} scope (2.2): every instance gets a unique {@code id}, so tests can tell "same
 * instance" from "new instance" without relying on {@code equals}.
 */
class Widget {

	private static final AtomicInteger SEQUENCE = new AtomicInteger();

	private final int id;
	private final String origin;

	Widget(final String origin) {
		this.id = SEQUENCE.incrementAndGet();
		this.origin = origin;
	}

	int id() {
		return id;
	}

	String origin() {
		return origin;
	}
}