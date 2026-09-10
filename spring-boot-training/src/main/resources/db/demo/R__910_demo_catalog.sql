-- Generated bulk catalog data for LOCAL development. The flyway-maven-plugin's <locations> in the
-- module pom lists both db/migration and db/demo, so `./mvnw -pl spring-boot-training flyway:migrate`
-- applies this against the local Oracle. The Testcontainers integration tests never see it: Boot's
-- Flyway (the only one active in the `test` profile) reads spring.flyway.locations = classpath:db/migration.
-- Purpose: enough rows to make U3.3 pagination, Sort, Specification filtering, and @EntityGraph /
-- N+1 examples meaningful.
--
-- REPEATABLE migration (R__): re-applied whenever its checksum changes, after all versioned ones,
-- in description order (so R__910 runs after R__010). Idempotent - it owns exactly the rows whose
-- sku starts with 'GEN-', deletes them, and regenerates them; DBMS_RANDOM.SEED keeps the generated
-- prices reproducible. Reset the local DB with
-- `./mvnw -pl spring-boot-training -Dflyway.cleanDisabled=false flyway:clean` then `flyway:migrate`.

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