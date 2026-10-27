package gr.codelearn.springbootai.persistencedemo.delivery;

/**
 * Kind of {@link Depot}. Persisted as its {@code name()} (readable, safe to reorder); {@code ck_depot_type} pins the column to these names.
 */
enum DepotType {
	HUB,
	LOCAL
}