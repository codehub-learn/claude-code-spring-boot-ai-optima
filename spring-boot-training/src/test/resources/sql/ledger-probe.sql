-- BEFORE_TEST_METHOD fixture for PersistenceRollbackIT#sqlScriptsBracketANonTransactionalTest.
-- The test is NOT @Transactional, so @Sql runs this in its own transaction and COMMITS it; the paired
-- ledger-probe-cleanup.sql (AFTER_TEST_METHOD) removes it. Marker: ledger_ref like 'SQLPROBE-%'.
insert into sales_ledger (ledger_ref, product_id, sold_on, quantity, net_amount)
values ('SQLPROBE-0001', (select id from product where sku = 'ACC-0001'), date '2026-07-01', 1, 42.00);
