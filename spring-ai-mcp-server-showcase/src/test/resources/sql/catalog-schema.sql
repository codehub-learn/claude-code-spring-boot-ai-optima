drop table if exists product;
drop table if exists category;

create table category
(
	id          number(19) generated always as identity primary key,
	name        varchar2(100) not null unique,
	description varchar2(500)
);

create table product
(
	id          number(19) generated always as identity primary key,
	name        varchar2(150) not null,
	description varchar2(1000),
	sku         varchar2(64)  not null unique,
	price       number(12, 2) not null,
	category_id number(19)    not null references category (id)
);