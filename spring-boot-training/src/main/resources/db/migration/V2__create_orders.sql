-- U3.2 - orders schema. Pairs with the catalog slice from V1 (category, product) and the U3.2 entities in
-- gr.codelearn.springbootai.training.catalog. Baseline seed: R__020_seed_orders.sql; demo bulk: db/demo/R__920_demo_orders.sql.
-- Conventions (CLAUDE.md, matching V1): snake_case, singular table names, pk_/fk_/ix_/uq_/ck_ constraint prefixes,
-- number(19) identity PKs, "default" before "not null", the created_at/updated_at/version trio on every table so the
-- AbstractAuditableEntity mapping (JPA auditing + @Version) lands without an ALTER.
-- "order" is a reserved word: the table name is quoted here and Hibernate emits it quoted too (@Table(name = "`order`")).

create table customer
(
	id          number(19) generated always as identity,
	first_name  varchar2(100)                                 not null,
	last_name   varchar2(100)                                 not null,
	email       varchar2(320)                                 not null,
	line1       varchar2(200)                                 not null,
	line2       varchar2(200),
	city        varchar2(120)                                 not null,
	postal_code varchar2(20)                                  not null,
	country     varchar2(2)                                   not null,
	created_at  timestamp with time zone default systimestamp not null,
	updated_at  timestamp with time zone default systimestamp not null,
	version     number(19)               default 0            not null,
	constraint pk_customer primary key (id),
	constraint uq_customer_email unique (email)
);

create table "order"
(
	id               number(19) generated always as identity,
	order_number     varchar2(40)                                  not null,
	customer_id      number(19)                                    not null,
	status           varchar2(20)                                  not null,
	total_amount     number(12, 2)            default 0            not null,
	placed_at        timestamp with time zone                      not null,
	ship_line1       varchar2(200)                                 not null,
	ship_line2       varchar2(200),
	ship_city        varchar2(120)                                 not null,
	ship_postal_code varchar2(20)                                  not null,
	ship_country     varchar2(2)                                   not null,
	created_at       timestamp with time zone default systimestamp not null,
	updated_at       timestamp with time zone default systimestamp not null,
	version          number(19)               default 0            not null,
	constraint pk_order primary key (id),
	constraint uq_order_number unique (order_number),
	constraint fk_order_customer foreign key (customer_id) references customer (id),
	constraint ck_order_status check (status in ('NEW', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

create table order_item
(
	id         number(19) generated always as identity,
	order_id   number(19)                                    not null,
	product_id number(19)                                    not null,
	quantity   number(10)                                    not null,
	unit_price number(12, 2)                                 not null,
	created_at timestamp with time zone default systimestamp not null,
	updated_at timestamp with time zone default systimestamp not null,
	version    number(19)               default 0            not null,
	constraint pk_order_item primary key (id),
	constraint uq_order_item_order_product unique (order_id, product_id),
	constraint fk_order_item_order foreign key (order_id) references "order" (id),
	constraint fk_order_item_product foreign key (product_id) references product (id),
	constraint ck_order_item_quantity_positive check (quantity > 0)
);

-- Oracle does not index foreign-key columns automatically. These support the "orders for customer X" /
-- "lines for order Y" reads and keep the FKs lock-free on a parent delete.
create index ix_order_customer on "order" (customer_id);
create index ix_order_item_order on order_item (order_id);
create index ix_order_item_product on order_item (product_id);