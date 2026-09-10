-- U3.5 - reporting read-model. A deliberately non-J|

-- Data JDBC, not Hibernate. The catalog/orders schema (V1, V2) stays the system of record; this is a separate
-- reporting store, fed in bulk and queried by aggregate.
-- Conventions (CLAUDE.md, matching V1/V2): snake_case, singular table names, pk_/fk_/ix_/uq_/ck_ prefixes,
-- number(19) identity PKs, "default" before "not null", an ix_ per FK column.
-- Baseline seed: R__030_seed_reporting.sql. No demo bulk - the ingest test generates its own volume.
--
-- Two deliberate departures from the V1/V2 shape, both because there is no JPA entity here:
--   * sales_ledger has NO created_at/updated_at/version trio (nothing maps it, nothing audits it).
--   * captured_at is a plain "timestamp", not "timestamp with time zone" - it pairs with a LocalDateTime
--     field, sidestepping the Instant/OffsetDateTime auditing-converter trap hit in U3.2.

-- Denormalized sales fact table. Fed by a batch insert (SalesIngestService, JdbcClient batch), read by
-- aggregate queries (CategorySalesQuery). product_id is a SOFT reference: indexed for the join, but no FK -
-- a reporting store tolerates a purged product row rather than blocking on it. ledger_ref is the natural key
-- the repeatable seed and any re-import MERGE on.
create table sales_ledger
(
	id         number(19) generated always as identity,
	ledger_ref varchar2(40)  not null,
	product_id number(19)    not null,
	sold_on    date          not null,
	quantity   number(10)    not null,
	net_amount number(14, 2) not null,
	constraint pk_sales_ledger primary key (id),
	constraint uq_sales_ledger_ref unique (ledger_ref),
	constraint ck_sales_ledger_quantity_positive check (quantity > 0)
);

create index ix_sales_ledger_product on sales_ledger (product_id);
create index ix_sales_ledger_sold_on on sales_ledger (sold_on);

-- A Spring Data JDBC aggregate: report_snapshot is the root, report_snapshot_line its owned children. Spring
-- Data JDBC loads and stores the whole aggregate together - no lazy loading, no dirty tracking. A snapshot
-- freezes one CategorySalesQuery result with a label and the date it covers from.
create table report_snapshot
(
	id          number(19) generated always as identity,
	label       varchar2(80)                   not null,
	from_date   date                           not null,
	captured_at timestamp default systimestamp not null,
	constraint pk_report_snapshot primary key (id),
	constraint uq_report_snapshot_label unique (label)
);

-- No id column of its own: a snapshot line has no identity outside its snapshot. Spring Data JDBC manages
-- snapshot_id as the aggregate back-reference and, on re-save, deletes every line for the snapshot and
-- re-inserts. The unique (snapshot_id, category_name) is the business key.
create table report_snapshot_line
(
	snapshot_id   number(19)    not null,
	category_name varchar2(100) not null,
	units_sold    number(19)    not null,
	gross_revenue number(14, 2) not null,
	constraint fk_report_snapshot_line_snapshot foreign key (snapshot_id) references report_snapshot (id),
	constraint uq_report_snapshot_line unique (snapshot_id, category_name)
);

create index ix_report_snapshot_line_snapshot on report_snapshot_line (snapshot_id);