-- Dromeas courier delivery schema: depot, courier, customer, invoice, shipment, tracking_event.
-- Baseline seed: R__010..R__030. Bulk data: R__910..R__930 (all in db/migration, so every environment, tests included, gets them).
-- Conventions (CLAUDE.md): snake_case, singular table names, pk_/fk_/uq_/ck_/ix_ prefixes, number(19) identity PKs, "default" before
-- "not null". Every mutable table carries the created_at / updated_at / version trio so the JPA auditing + @Version mapping lands
-- without an ALTER. tracking_event is an append-only log: created_at only, no update, no version.
-- Each business entity also has a human-readable natural key (code, employee_code, customer_code, tracking_number, invoice_number);
-- the seed and bulk scripts MERGE on it, which is what makes them idempotent.
-- Timestamps are `timestamp with time zone`; the bulk data is generated in Europe/Athens local time.

create table depot
(
	id           number(19) generated always as identity,
	code         varchar2(10)                                  not null,
	name         varchar2(120)                                 not null,
	depot_type   varchar2(10)                                  not null,
	city         varchar2(120)                                 not null,
	region       varchar2(120)                                 not null,
	address_line varchar2(200)                                 not null,
	postal_code  varchar2(20)                                  not null,
	opened_on    date                                          not null,
	created_at   timestamp with time zone default systimestamp not null,
	updated_at   timestamp with time zone default systimestamp not null,
	version      number(19)               default 0            not null,
	constraint pk_depot primary key (id),
	constraint uq_depot_code unique (code),
	constraint ck_depot_type check (depot_type in ('HUB', 'LOCAL'))
);

create table courier
(
	id             number(19) generated always as identity,
	depot_id       number(19)                                    not null,
	employee_code  varchar2(20)                                  not null,
	first_name     varchar2(100)                                 not null,
	last_name      varchar2(100)                                 not null,
	phone          varchar2(30)                                  not null,
	vehicle_type   varchar2(20)                                  not null,
	daily_capacity number(4)                                     not null,
	active         number(1)                default 1            not null,
	hired_on       date                                          not null,
	created_at     timestamp with time zone default systimestamp not null,
	updated_at     timestamp with time zone default systimestamp not null,
	version        number(19)               default 0            not null,
	constraint pk_courier primary key (id),
	constraint uq_courier_employee_code unique (employee_code),
	constraint fk_courier_depot foreign key (depot_id) references depot (id),
	constraint ck_courier_vehicle_type check (vehicle_type in ('MOTORBIKE', 'CARGO_BIKE', 'VAN', 'TRUCK')),
	constraint ck_courier_daily_capacity check (daily_capacity > 0),
	constraint ck_courier_active check (active in (0, 1))
);

-- A customer is either a private person (first/last name) or a business (company name + 9-digit Greek VAT number, "AFM").
-- depot_id is the depot that serves the customer's pickups.
create table customer
(
	id            number(19) generated always as identity,
	customer_code varchar2(20)                                  not null,
	customer_type varchar2(10)                                  not null,
	first_name    varchar2(100),
	last_name     varchar2(100),
	company_name  varchar2(200),
	vat_number    varchar2(9),
	contract_tier varchar2(10)             default 'STANDARD'   not null,
	email         varchar2(320)                                 not null,
	phone         varchar2(30)                                  not null,
	depot_id      number(19)                                    not null,
	line1         varchar2(200)                                 not null,
	city          varchar2(120)                                 not null,
	postal_code   varchar2(20)                                  not null,
	country       varchar2(2)              default 'GR'         not null,
	created_at    timestamp with time zone default systimestamp not null,
	updated_at    timestamp with time zone default systimestamp not null,
	version       number(19)               default 0            not null,
	constraint pk_customer primary key (id),
	constraint uq_customer_code unique (customer_code),
	constraint uq_customer_email unique (email),
	constraint uq_customer_vat_number unique (vat_number),
	constraint fk_customer_depot foreign key (depot_id) references depot (id),
	constraint ck_customer_type check (customer_type in ('PRIVATE', 'BUSINESS')),
	constraint ck_customer_contract_tier check (contract_tier in ('STANDARD', 'SILVER', 'GOLD')),
	constraint ck_customer_identity check (
		(customer_type = 'PRIVATE' and first_name is not null and last_name is not null and company_name is null and vat_number is null)
			or (customer_type = 'BUSINESS' and company_name is not null and vat_number is not null)
		)
);

-- Monthly invoice per business customer, covering that customer's delivered shipments of the period (shipment.invoice_id).
create table invoice
(
	id             number(19) generated always as identity,
	invoice_number varchar2(30)                                  not null,
	customer_id    number(19)                                    not null,
	period_start   date                                          not null,
	period_end     date                                          not null,
	issued_on      date                                          not null,
	due_on         date                                          not null,
	net_amount     number(12, 2)                                 not null,
	vat_amount     number(12, 2)                                 not null,
	total_amount   number(12, 2)                                 not null,
	status         varchar2(10)                                  not null,
	paid_on        date,
	created_at     timestamp with time zone default systimestamp not null,
	updated_at     timestamp with time zone default systimestamp not null,
	version        number(19)               default 0            not null,
	constraint pk_invoice primary key (id),
	constraint uq_invoice_number unique (invoice_number),
	constraint uq_invoice_customer_period unique (customer_id, period_start),
	constraint fk_invoice_customer foreign key (customer_id) references customer (id),
	constraint ck_invoice_status check (status in ('ISSUED', 'PAID', 'OVERDUE')),
	constraint ck_invoice_period check (period_end >= period_start),
	constraint ck_invoice_paid_on check ((status = 'PAID' and paid_on is not null) or (status <> 'PAID' and paid_on is null))
);

-- courier_id stays null until the shipment is dispatched (out for delivery); invoice_id stays null until it is invoiced.
-- picked_up_at / delivered_at are denormalised from tracking_event for cheap reads. attempts counts out-for-delivery runs.
create table shipment
(
	id                    number(19) generated always as identity,
	tracking_number       varchar2(12)                                  not null,
	customer_id           number(19)                                    not null,
	origin_depot_id       number(19)                                    not null,
	destination_depot_id  number(19)                                    not null,
	courier_id            number(19),
	invoice_id            number(19),
	service_level         varchar2(10)                                  not null,
	status                varchar2(20)             default 'CREATED'    not null,
	weight_kg             number(7, 2)                                  not null,
	price                 number(10, 2)                                 not null,
	recipient_name        varchar2(200)                                 not null,
	recipient_line1       varchar2(200)                                 not null,
	recipient_city        varchar2(120)                                 not null,
	recipient_postal_code varchar2(20)                                  not null,
	picked_up_at          timestamp with time zone,
	promised_by           timestamp with time zone                      not null,
	delivered_at          timestamp with time zone,
	attempts              number(2)                default 0            not null,
	created_at            timestamp with time zone default systimestamp not null,
	updated_at            timestamp with time zone default systimestamp not null,
	version               number(19)               default 0            not null,
	constraint pk_shipment primary key (id),
	constraint uq_shipment_tracking_number unique (tracking_number),
	constraint fk_shipment_customer foreign key (customer_id) references customer (id),
	constraint fk_shipment_origin_depot foreign key (origin_depot_id) references depot (id),
	constraint fk_shipment_destination_depot foreign key (destination_depot_id) references depot (id),
	constraint fk_shipment_courier foreign key (courier_id) references courier (id),
	constraint fk_shipment_invoice foreign key (invoice_id) references invoice (id),
	constraint ck_shipment_service_level check (service_level in ('STANDARD', 'EXPRESS', 'SAME_DAY')),
	constraint ck_shipment_status check (status in ('CREATED', 'PICKED_UP', 'AT_HUB', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED_ATTEMPT')),
	constraint ck_shipment_weight_positive check (weight_kg > 0),
	constraint ck_shipment_price_non_negative check (price >= 0),
	constraint ck_shipment_attempts_non_negative check (attempts >= 0)
);

create table tracking_event
(
	id          number(19) generated always as identity,
	shipment_id number(19)                                    not null,
	depot_id    number(19)                                    not null,
	status      varchar2(20)                                  not null,
	occurred_at timestamp with time zone                      not null,
	note        varchar2(200),
	created_at  timestamp with time zone default systimestamp not null,
	constraint pk_tracking_event primary key (id),
	constraint fk_tracking_event_shipment foreign key (shipment_id) references shipment (id),
	constraint fk_tracking_event_depot foreign key (depot_id) references depot (id),
	constraint ck_tracking_event_status check (status in ('PICKED_UP', 'AT_HUB', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED_ATTEMPT'))
);

-- Oracle does not index foreign-key columns automatically. These support the join and lookup paths the demo queries use and keep
-- the FKs lock-free on a parent delete. uq_invoice_customer_period already covers invoice.customer_id.
create index ix_courier_depot on courier (depot_id);
create index ix_customer_depot on customer (depot_id);
create index ix_shipment_customer on shipment (customer_id);
create index ix_shipment_origin_depot on shipment (origin_depot_id);
create index ix_shipment_destination_depot on shipment (destination_depot_id);
create index ix_shipment_courier on shipment (courier_id);
create index ix_shipment_invoice on shipment (invoice_id);
create index ix_shipment_status_created_at on shipment (status, created_at);
create index ix_tracking_event_shipment_time on tracking_event (shipment_id, occurred_at);
create index ix_tracking_event_depot on tracking_event (depot_id);