package gr.codelearn.springbootai.training.order.dto;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Marker interfaces for {@link JsonView} on {@link OrderDetailResponse} (U4.3). {@link Detail} {@code extends} {@link Summary}: a field
 * tagged {@code @JsonView(Summary.class)} appears whichever view is active, a field tagged {@code @JsonView(Detail.class)} only when
 * {@link Detail} itself is the active view — so one record can serve both the list/summary endpoints and the single-order detail endpoint,
 * with no second DTO and no duplicated fields.
 *
 * <p>The active view is selected with {@code @JsonView} directly on the {@code OrderController} handler method; Spring MVC's built-in
 * {@code JsonViewResponseBodyAdvice} applies it, no manual {@code MappingJacksonValue} wrapping needed.
 */
public final class Views {

	private Views() {
	}

	/**
	 * The shape every order endpoint returns: {@code orderNumber}, {@code status}, {@code total}, {@code lineCount}.
	 */
	public interface Summary {
	}

	/**
	 * Adds {@code customerEmail}, {@code placedAt}, {@code shippingAddress}, and the full {@code lines} — only
	 * {@code GET /orders/{orderNumber}} uses this view.
	 */
	public interface Detail extends Summary {
	}
}