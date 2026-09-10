-- AFTER_TEST_METHOD teardown paired with ledger-probe.sql. Committed, like its fixture.
delete
from sales_ledger
where ledger_ref like 'SQLPROBE-%';