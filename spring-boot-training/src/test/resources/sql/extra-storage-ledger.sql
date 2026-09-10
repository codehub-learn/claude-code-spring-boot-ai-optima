-- @Sql fixture for CategorySalesQueryJdbcTest. Adds Storage-category sales dated 2026-03-01, after the
-- R__030 seed window (Jan/Feb), so an aggregate query over a March window sees only these two rows:
-- 10 units, 2500.00 gross, line_count 2. Runs inside the @JdbcTest transaction and rolls back with it.
insert into sales_ledger (ledger_ref, product_id, sold_on, quantity, net_amount)
values ('SQL-L-0001', (select id from product where sku = 'STO-0001'), date '2026-03-01', 4, 1000.00);
insert into sales_ledger (ledger_ref, product_id, sold_on, quantity, net_amount)
values ('SQL-L-0002', (select id from product where sku = 'STO-0002'), date '2026-03-01', 6, 1500.00);
