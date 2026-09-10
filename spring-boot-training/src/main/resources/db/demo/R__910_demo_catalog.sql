-- Demo-only bulk catalog data. NOT in db/migration, so a plain `flyway:migrate` and the Boot-managed
-- Flyway in the `test` profile never see it. Opt in against a local Oracle only:
--
--   ./mvnw -pl spring-boot-training flyway:migrate -Pdemo
--
-- The `demo` Maven profile appends src/main/resources/db/demo to flyway.locations. Purpose: enough
-- rows to make U3.3 pagination, Sort, Specification filtering, and @EntityGraph / N+1 examples
-- meaningful.
--
-- Idempotent: it owns exactly the rows whose sku starts with 'GEN-', deletes them, and regenerates
-- them. DBMS_RANDOM.SEED makes the generated prices reproducible across runs. As a REPEATABLE
-- migration Flyway re-applies it whenever the checksum changes. After running with -Pdemo, a later
-- plain `flyway:migrate` reports R__910 as a missing repeatable (a warning, not a failure);
-- `./mvnw -pl spring-boot-training flyway:clean flyway:migrate` returns the database to baseline.

begin
dbms_random.seed(4242);
end;
/

delete
from product
where sku like 'GEN-%';

insert into product (name, description, sku, price, category_id)
with cats as (select id, row_number() over (order by id) - 1 as rn, count(*) over () as cnt
			  from category),
	 gen as (select level as n
			 from dual connect by level <= 500)
select 'Demo Product ' || to_char(g.n),
	   'Auto-generated demo row for pagination / filtering / N+1 examples',
	   'GEN-' || lpad(to_char(g.n), 6, '0'),
	   round(dbms_random.value(4, 2999), 2),
	   c.id
from gen g
		 join cats c on c.rn = mod(g.n, c.cnt);