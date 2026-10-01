insert into category (name)
values ('Laptops');
insert into category (name)
values ('Audio');
insert into category (name)
values ('Empty');

insert into product (name, description, sku, price, category_id)
values ('UltraBook 14', 'Light 14 inch laptop with a 100% recycled case', 'LAP-001', 1299.00,
        (select id from category where name = 'Laptops'));
insert into product (name, description, sku, price, category_id)
values ('WorkStation 16', 'Powerful 16 inch laptop for builds', 'LAP-002', 2199.00, (select id from category where name = 'Laptops'));
insert into product (name, description, sku, price, category_id)
values ('Studio Headphones', 'Closed-back headphones, 20_000 hours rated', 'AUD-001', 149.90,
        (select id from category where name = 'Audio'));
insert into product (name, description, sku, price, category_id)
values ('Generated Filler', 'Placeholder row that must never show up', 'GEN-0001', 1.00, (select id from category where name = 'Audio'));