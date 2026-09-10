-- Generated bulk orders data for LOCAL development. Same contract as R__910_demo_catalog.sql: the
-- flyway-maven-plugin's <locations> lists db/demo, so `./mvnw -pl spring-boot-training flyway:migrate`
-- applies it against the local Oracle; the Testcontainers integration tests never see it (Boot's Flyway
-- reads spring.flyway.locations = classpath:db/migration).
-- Purpose: enough orders to make U3.3 pagination / Sort / Specification / @EntityGraph examples meaningful
-- on the order aggregate.
--
-- REPEATABLE migration (R__): re-applied on checksum change, after all versioned ones and after R__910
-- (description order). Idempotent - it owns exactly the orders whose order_number starts with 'GEN-',
-- deletes them (children first) and regenerates them; DBMS_RANDOM.SEED keeps the output reproducible.
-- Uses the customers from R__020 and whatever products exist (baseline + R__910). "order" is quoted - it
-- is a reserved word (see V2).

begin
dbms_random.seed(4242);
end;
/

delete
from order_item
where order_id in (select id from "order" where order_number like 'GEN-%');

delete
from "order"
where order_number like 'GEN-%';

insert into "order" (order_number, customer_id, status, total_amount, placed_at, ship_line1, ship_city,
					 ship_postal_code, ship_country)
with cust as (select id, row_number() over (order by id) as rn, count(*) over () as cnt
			  from customer),
	 gen as (select level as n
			 from dual connect by level <= 200)
select 'GEN-' || lpad(to_char(g.n), 6, '0'),
	   c.id,
	   case trunc(dbms_random.value(0, 5))
		   when 0 then 'NEW'
		   when 1 then 'PAID'
		   when 2 then 'SHIPPED'
		   when 3 then 'DELIVERED'
		   else 'CANCELLED'
		   end,
	   0,
	   systimestamp - numtodsinterval(dbms_random.value(0, 365), 'DAY'),
	   to_char(trunc(dbms_random.value(1, 999))) || ' Demo Street',
	   'Demo City',
	   '00000',
	   'GR'
from gen g
		 join cust c on c.rn = mod(g.n, c.cnt) + 1;

-- 1..3 distinct lines per order. Product picked by a fixed offset per (order, line) so the
-- (order_id, product_id) unique constraint is never hit; quantity is the only random part.
insert into order_item (order_id, product_id, quantity, unit_price)
with prod as (select id, price, row_number() over (order by id) as rn, count(*) over () as cnt
			  from product),
	 ord as (select id, row_number() over (order by id) as rn
			 from "order"
			 where order_number like 'GEN-%'),
	 nums as (select level as ln
			  from dual connect by level <= 3)
select o.id,
	   p.id,
	   trunc(dbms_random.value(1, 6)),
	   p.price
from ord o
		 join nums n on n.ln <= 1 + mod(o.rn, 3)
		 join prod p on p.rn = 1 + mod(o.rn * 7 + n.ln * 13, p.cnt);

-- Cache each generated order's total from its lines.
update "order" o
set o.total_amount = (select nvl(sum(oi.quantity * oi.unit_price), 0)
					  from order_item oi
					  where oi.order_id = o.id)
where o.order_number like 'GEN-%';