-- Unit 10.1 fixture for KnowledgeApiIT: a minimal copy of spring-boot-training's catalog tables in the test schema (the Testcontainers
-- user), where app.ai.catalog.schema points under the test profile. Only the columns CatalogDocumentReader and ProductService read.
-- Idempotent: the container is shared by every IT in the JVM.
create table if not exists category
(
	id   number(19) primary key,
	name varchar2(100) not null
);

create table if not exists product
(
	id          number(19) primary key,
	sku         varchar2(64)  not null,
	name        varchar2(150) not null,
	description varchar2(1000),
	price       number(12, 2) not null,
	category_id number(19)    not null references category (id)
);

delete
from product;

delete
from category;

insert into category (id, name)
values (1, 'Laptops');

insert into category (id, name)
values (2, 'Audio');

insert into product (id, sku, name, description, price, category_id)
values (1, 'LAP-0002', 'Latitude 16 Workstation', '16" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU', 2199.00, 1);

insert into product (id, sku, name, description, price, category_id)
values (2, 'LAP-0003', 'Flex 13 Convertible', '13" 2-in-1 convertible with pen support', 1099.00, 1);

insert into product (id, sku, name, description, price, category_id)
values (3, 'AUD-0001', 'Studio 3 Over-Ear', 'Wireless over-ear headphones, active noise cancelling', 299.00, 2);

-- A generated demo row: ingestion must skip it.
insert into product (id, sku, name, description, price, category_id)
values (4, 'GEN-000001', 'Demo Product 1', 'Auto-generated demo row for pagination / filtering / N+1 examples', 10.00, 2);