-- Baseline orders seed. REPEATABLE migration (R__), same contract as R__010_seed_catalog.sql: Flyway re-runs it
-- after every versioned migration whenever this file's checksum changes, so it evolves in the same commit as the
-- V2 DDL and the U3.2 entities. It lives in db/migration, so it is applied everywhere the schema is - both
-- `./mvnw flyway:migrate` and the Boot-managed Flyway in the `test` profile (Testcontainers).
--
-- Keep it SMALL: it is the fixture every orders-touching integration test starts from. The large local-only
-- dataset is db/demo/R__920_demo_orders.sql.
--
-- Idempotent by construction: one MERGE per table keyed on a natural key (customer.email, "order".order_number,
-- order_item on the (order, product) pair). Identity `id` columns are never inserted; created_at / updated_at /
-- version fall to their DDL defaults. Line prices are read from the product row so they track R__010.
--
-- Numeric prefix in the description ("020") orders it after R__010_seed_catalog.sql, which it depends on for the
-- product and category rows. "order" is quoted throughout - it is a reserved word (see V2).

merge into customer tgt
using (select 'alice@example.com'         as email,
			  'Alice'                     as first_name,
			  'Anderson'                  as last_name,
			  '221B Baker Street'         as line1,
			  cast(null as varchar2(200)) as line2,
			  'London'                    as city,
			  'NW1 6XE'                   as postal_code,
			  'GB'                        as country
	   from dual
	   union all
	   select 'bob@example.com',
			  'Bob',
			  'Brown',
			  '1600 Amphitheatre Pkwy',
			  null,
			  'Mountain View',
			  '94043',
			  'US'
	   from dual
	   union all
	   select 'carol@example.com',
			  'Carol',
			  'Clark',
			  'Panepistimiou 30',
			  null,
			  'Athens',
			  '10679',
			  'GR'
	   from dual) src
on (tgt.email = src.email)
when matched then
	update
	set tgt.first_name  = src.first_name,
		tgt.last_name   = src.last_name,
		tgt.line1       = src.line1,
		tgt.line2       = src.line2,
		tgt.city        = src.city,
		tgt.postal_code = src.postal_code,
		tgt.country     = src.country
when not matched then
	insert (first_name, last_name, email, line1, line2, city, postal_code, country)
	values (src.first_name, src.last_name, src.email, src.line1, src.line2, src.city, src.postal_code, src.country);

merge into "order" tgt
using (with seed (order_number, email, status, placed_offset_days, ship_line1, ship_line2, ship_city,
				  ship_postal_code, ship_country) as
				(select 'SEED-0001',
						'alice@example.com',
						'PAID',
						10,
						'221B Baker Street',
						cast(null as varchar2(200)),
						'London',
						'NW1 6XE',
						'GB'
				 from dual
				 union all
				 select 'SEED-0002',
						'bob@example.com',
						'SHIPPED',
						5,
						'1600 Amphitheatre Pkwy',
						null,
						'Mountain View',
						'94043',
						'US'
				 from dual
				 union all
				 select 'SEED-0003',
						'carol@example.com',
						'NEW',
						1,
						'Panepistimiou 30',
						null,
						'Athens',
						'10679',
						'GR'
				 from dual)
	   select s.order_number,
			  s.status,
			  s.ship_line1,
			  s.ship_line2,
			  s.ship_city,
			  s.ship_postal_code,
			  s.ship_country,
			  c.id                                                        as customer_id,
			  systimestamp - numtodsinterval(s.placed_offset_days, 'DAY') as placed_at
	   from seed s
				join customer c on c.email = s.email) src
on (tgt.order_number = src.order_number)
when matched then
	update
	set tgt.status           = src.status,
		tgt.customer_id      = src.customer_id,
		tgt.ship_line1       = src.ship_line1,
		tgt.ship_line2       = src.ship_line2,
		tgt.ship_city        = src.ship_city,
		tgt.ship_postal_code = src.ship_postal_code,
		tgt.ship_country     = src.ship_country
when not matched then
	insert (order_number, customer_id, status, total_amount, placed_at, ship_line1, ship_line2, ship_city,
			ship_postal_code, ship_country)
	values (src.order_number, src.customer_id, src.status, 0, src.placed_at, src.ship_line1, src.ship_line2,
			src.ship_city, src.ship_postal_code, src.ship_country);

merge into order_item tgt
using (with lines (order_number, sku, quantity) as
				(select 'SEED-0001', 'LAP-0001', 1
				 from dual
				 union all
				 select 'SEED-0001', 'AUD-0002', 2
				 from dual
				 union all
				 select 'SEED-0002', 'PHN-0001', 1
				 from dual
				 union all
				 select 'SEED-0002', 'ACC-0004', 1
				 from dual
				 union all
				 select 'SEED-0003', 'MON-0002', 2
				 from dual)
	   select o.id as order_id, p.id as product_id, l.quantity, p.price as unit_price
	   from lines l
				join "order" o on o.order_number = l.order_number
				join product p on p.sku = l.sku) src
on (tgt.order_id = src.order_id and tgt.product_id = src.product_id)
when matched then
	update
	set tgt.quantity   = src.quantity,
		tgt.unit_price = src.unit_price
when not matched then
	insert (order_id, product_id, quantity, unit_price)
	values (src.order_id, src.product_id, src.quantity, src.unit_price);

-- Keep the cached order total in step with the lines just merged.
update "order" o
set o.total_amount = (select nvl(sum(oi.quantity * oi.unit_price), 0)
					  from order_item oi
					  where oi.order_id = o.id)
where o.order_number like 'SEED-%';