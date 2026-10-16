package gr.codelearn.springbootai.coredemo.order;

public record OrderLineRequest(long productId, int quantity) {
}
