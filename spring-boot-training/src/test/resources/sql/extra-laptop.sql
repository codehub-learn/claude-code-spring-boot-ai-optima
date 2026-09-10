-- @Sql fixture for ProductRepositoryDataJpaTest. Lives under src/test/resources/sql, NOT db/migration,
-- so Boot's Flyway never picks it up. It runs inside the @DataJpaTest transaction and is rolled back
-- with the test method. created_at / updated_at / version fall to their DDL defaults (V1).
insert into product (sku, name, price, category_id)
values ('TST-9001', 'Test Bench Laptop', 999.00,
		(select id from category where name = 'Laptops'));