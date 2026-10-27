package gr.codelearn.springbootai.persistencedemo.depot;

/**
 * Kind of {@link Depot}. Persisted as its {@code name()} (readable, safe to reorder); {@code ck_depot_type} pins the column to these names.
 */
public enum DepotType {
	HUB,
	LOCAL
}