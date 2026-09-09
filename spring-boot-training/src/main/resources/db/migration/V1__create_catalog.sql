-- U3.1 - catalog slice. The orders schema (customer, "order", order_item) lands in V2 with its entities.
-- Conventions (CLAUDE.md): snake_case, singular table names, pk_/fk_/ix_/uq_/ck_ constraint prefixes.
-- created_at / updated_at / version exist from the start so the U3.2 entities (JPA auditing + @Version)
-- map onto them without an ALTER; the DB defaults fill them until JPA auditing takes over.
-- Oracle syntax: number/varchar2, "timestamp with time zone", systimestamp, "default" before "not null".

create table category
(
	id          number(19) generated always as identity,
	name        varchar2(100)                                 not null,
	description varchar2(500),
	created_at  timestamp with time zone default systimestamp not null,
	updated_at  timestamp with time zone default systimestamp not null,
	version     number(19)               default 0            not null,
	constraint pk_category primary key (id),
	constraint uq_category_name unique (name)
);

create table product
(
	id          number(19) generated always as identity,
	name        varchar2(150)                                 not null,
	description varchar2(1000),
	sku         varchar2(64)                                  not null,
	price       number(12, 2)                                 not null,
	category_id number(19)                                    not null,
	created_at  timestamp with time zone default systimestamp not null,
	updated_at  timestamp with time zone default systimestamp not null,
	version     number(19)               default 0            not null,
	constraint pk_product primary key (id),
	constraint uq_product_sku unique (sku),
	constraint fk_product_category foreign key (category_id) references category (id),
	constraint ck_product_price_non_negative check (price >= 0)
);

-- Oracle does not index foreign-key columns automatically; this one supports "products in category X"
-- and keeps the FK lock-free on a category delete.
create index ix_product_category on product (category_id);