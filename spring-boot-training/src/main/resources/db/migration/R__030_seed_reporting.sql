-- Baseline reporting seed. REPEATABLE migration (R__), same contract as R__010 / R__020: Flyway re-runs it after
-- every versioned migration whenever this file's checksum changes, so it evolves in the same commit as the V3
-- DDL and the U3.5 reporting slice. It lives in db/migration, so it is applied everywhere the schema is - both
-- `./mvnw flyway:migrate` and the Boot-managed Flyway in the `test` profile (Testcontainers).
--
-- Keep it SMALL: it is the fixture every reporting integration test starts from. There is no demo bulk tier for
-- reporting - SalesIngestServiceIT generates its own volume with the batch insert under test.
--
-- Idempotent by construction: one MERGE per table on a natural key (sales_ledger.ledger_ref,
-- report_snapshot.label, report_snapshot_line on the (snapshot_id, category_name) pair). Identity `id` columns
-- are never inserted.
--
-- Numeric prefix ("030") orders it after R__020_seed_orders.sql; it depends on R__010 for the product / category
-- rows. The ledger spans two months so the `since` filter in CategorySalesQuery has something to exclude, and
-- each report_snapshot_line total equals the sum of its category's two ledger rows (ReportingSeedIT asserts it).

merge into sales_ledger tgt
using (with seed (ledger_ref, sku, sold_on, quantity, net_amount) as
				(select 'SEED-L-0001', 'LAP-0001', date '2026-01-15', 1, 1299.00
				 from dual
				 union all
				 select 'SEED-L-0002', 'LAP-0002', date '2026-02-20', 2, 4398.00
				 from dual
				 union all
				 select 'SEED-L-0003', 'PHN-0001', date '2026-01-15', 3, 2397.00
				 from dual
				 union all
				 select 'SEED-L-0004', 'PHN-0002', date '2026-02-20', 1, 1099.00
				 from dual
				 union all
				 select 'SEED-L-0005', 'AUD-0001', date '2026-01-15', 2, 698.00
				 from dual
				 union all
				 select 'SEED-L-0006', 'AUD-0002', date '2026-02-20', 4, 916.00
				 from dual
				 union all
				 select 'SEED-L-0007', 'MON-0001', date '2026-01-15', 1, 179.00
				 from dual
				 union all
				 select 'SEED-L-0008', 'MON-0002', date '2026-02-20', 3, 987.00
				 from dual
				 union all
				 select 'SEED-L-0009', 'STO-0001', date '2026-01-15', 5, 495.00
				 from dual
				 union all
				 select 'SEED-L-0010', 'STO-0002', date '2026-02-20', 2, 338.00
				 from dual
				 union all
				 select 'SEED-L-0011', 'ACC-0001', date '2026-01-15', 1, 119.00
				 from dual
				 union all
				 select 'SEED-L-0012', 'ACC-0002', date '2026-02-20', 2, 158.00
				 from dual)
	   select s.ledger_ref, p.id as product_id, s.sold_on, s.quantity, s.net_amount
	   from seed s
				join product p on p.sku = s.sku) src
on (tgt.ledger_ref = src.ledger_ref)
when matched then
	update
	set tgt.product_id = src.product_id,
		tgt.sold_on    = src.sold_on,
		tgt.quantity   = src.quantity,
		tgt.net_amount = src.net_amount
when not matched then
	insert (ledger_ref, product_id, sold_on, quantity, net_amount)
	values (src.ledger_ref, src.product_id, src.sold_on, src.quantity, src.net_amount);

merge into report_snapshot tgt
using (select 'baseline' as label, date '2026-01-01' as from_date
	   from dual) src
on (tgt.label = src.label)
when matched then
	update
	set tgt.from_date = src.from_date
when not matched then
	insert (label, from_date)
	values (src.label, src.from_date);

merge into report_snapshot_line tgt
using (with lines (category_name, units_sold, gross_revenue) as
				(select 'Laptops', 3, 5697.00
				 from dual
				 union all
				 select 'Smartphones', 4, 3496.00
				 from dual
				 union all
				 select 'Audio', 6, 1614.00
				 from dual
				 union all
				 select 'Monitors', 4, 1166.00
				 from dual
				 union all
				 select 'Storage', 7, 833.00
				 from dual
				 union all
				 select 'Accessories', 3, 277.00
				 from dual)
	   select (select id from report_snapshot where label = 'baseline') as snapshot_id,
			  l.category_name,
			  l.units_sold,
			  l.gross_revenue
	   from lines l) src
on (tgt.snapshot_id = src.snapshot_id and tgt.category_name = src.category_name)
when matched then
	update
	set tgt.units_sold    = src.units_sold,
		tgt.gross_revenue = src.gross_revenue
when not matched then
	insert (snapshot_id, category_name, units_sold, gross_revenue)
	values (src.snapshot_id, src.category_name, src.units_sold, src.gross_revenue);