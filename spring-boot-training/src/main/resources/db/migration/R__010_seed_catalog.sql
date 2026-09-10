-- Baseline catalog seed. REPEATABLE migration (R__): Flyway re-runs it after every versioned
-- migration whenever this file's checksum changes, so it evolves in the same commit as the V* DDL
-- and the U3.2 entities. It lives in db/migration, so it is applied everywhere the schema is -
-- `./mvnw flyway:migrate` against local Oracle AND the Boot-managed Flyway in the `test` profile
-- (Testcontainers). Keep it SMALL: it is the fixture every integration test starts from.
--
-- Idempotent by construction: one MERGE per table keyed on the natural key (category.name,
-- product.sku). Identity `id` columns are never inserted; created_at / updated_at / version fall to
-- their DDL defaults. The large local-only dataset is db/demo/R__910_demo_catalog.sql.
--
-- Numeric prefix in the description ("010") orders it before R__020_seed_orders.sql (U3.2).

merge into category tgt
using (select 'Laptops' as name, 'Ultrabooks, mobile workstations, and 2-in-1 convertibles' as description
	   from dual
	   union all
	   select 'Smartphones', 'Android and iOS handsets, including foldables'
	   from dual
	   union all
	   select 'Audio', 'Headphones, earbuds, and portable speakers'
	   from dual
	   union all
	   select 'Monitors', 'Desktop displays from 24" office panels to 5K reference screens'
	   from dual
	   union all
	   select 'Storage', 'Internal and external SSDs, NVMe drives, and memory cards'
	   from dual
	   union all
	   select 'Accessories', 'Keyboards, mice, docks, cables, and chargers'
	   from dual) src
on (tgt.name = src.name)
when matched then
	update
	set tgt.description = src.description
when not matched then
	insert (name, description)
	values (src.name, src.description);

merge into product tgt
using (with seed (sku, name, category, price, description) as (select 'LAP-0001',
																	  'Latitude 14 Ultralight',
																	  'Laptops',
																	  1299.00,
																	  '14" business ultrabook, 16 GB RAM, 512 GB SSD'
															   from dual
															   union all
															   select 'LAP-0002',
																	  'Latitude 16 Workstation',
																	  'Laptops',
																	  2199.00,
																	  '16" mobile workstation, 32 GB RAM, 1 TB SSD, discrete GPU'
															   from dual
															   union all
															   select 'LAP-0003',
																	  'Flex 13 Convertible',
																	  'Laptops',
																	  1099.00,
																	  '13" 2-in-1 convertible with pen support'
															   from dual
															   union all
															   select 'LAP-0004',
																	  'Nomad 15 Creator',
																	  'Laptops',
																	  1749.00,
																	  '15" creator laptop, 4K OLED, 1 TB SSD'
															   from dual
															   union all
															   select 'PHN-0001',
																	  'Pixelia 8',
																	  'Smartphones',
																	  799.00,
																	  '6.2" flagship, 128 GB, dual camera'
															   from dual
															   union all
															   select 'PHN-0002',
																	  'Pixelia 8 Pro',
																	  'Smartphones',
																	  1099.00,
																	  '6.7" flagship, 256 GB, triple camera, telephoto'
															   from dual
															   union all
															   select 'PHN-0003',
																	  'Aurora Fold 5',
																	  'Smartphones',
																	  1799.00,
																	  '7.6" inner foldable display, 512 GB'
															   from dual
															   union all
															   select 'PHN-0004',
																	  'Lite 6a',
																	  'Smartphones',
																	  349.00,
																	  '6.1" mid-range, 128 GB, long battery life'
															   from dual
															   union all
															   select 'AUD-0001',
																	  'Studio 3 Over-Ear',
																	  'Audio',
																	  349.00,
																	  'Wireless over-ear headphones, active noise cancelling'
															   from dual
															   union all
															   select 'AUD-0002',
																	  'AirBeat Pro Earbuds',
																	  'Audio',
																	  229.00,
																	  'In-ear ANC earbuds with wireless charging case'
															   from dual
															   union all
															   select 'AUD-0003',
																	  'Boom 2 Portable Speaker',
																	  'Audio',
																	  129.00,
																	  'IP67 portable Bluetooth speaker, 16 h battery'
															   from dual
															   union all
															   select 'AUD-0004',
																	  'Desk Reference Monitors',
																	  'Audio',
																	  449.00,
																	  'Pair of powered near-field studio monitors'
															   from dual
															   union all
															   select 'MON-0001',
																	  'ProView 24 FHD',
																	  'Monitors',
																	  179.00,
																	  '24" 1080p IPS office monitor, height adjustable'
															   from dual
															   union all
															   select 'MON-0002',
																	  'ProView 27 QHD',
																	  'Monitors',
																	  329.00,
																	  '27" 1440p IPS, 100 Hz, USB-C 65 W'
															   from dual
															   union all
															   select 'MON-0003',
																	  'ProView 32 4K',
																	  'Monitors',
																	  649.00,
																	  '32" 4K IPS, HDR400, USB-C hub'
															   from dual
															   union all
															   select 'MON-0004',
																	  'ProView 34 Ultrawide',
																	  'Monitors',
																	  799.00,
																	  '34" 3440x1440 curved ultrawide, 144 Hz'
															   from dual
															   union all
															   select 'STO-0001',
																	  'RapidNVMe 1 TB',
																	  'Storage',
																	  99.00,
																	  'PCIe 4.0 M.2 NVMe SSD, 7000 MB/s read'
															   from dual
															   union all
															   select 'STO-0002',
																	  'RapidNVMe 2 TB',
																	  'Storage',
																	  169.00,
																	  'PCIe 4.0 M.2 NVMe SSD, 7000 MB/s read'
															   from dual
															   union all
															   select 'STO-0003',
																	  'Voyager Portable SSD 2 TB',
																	  'Storage',
																	  199.00,
																	  'USB 3.2 Gen 2 external SSD, 1050 MB/s'
															   from dual
															   union all
															   select 'STO-0004',
																	  'Capture microSD 256 GB',
																	  'Storage',
																	  39.00,
																	  'UHS-I U3 V30 A2 memory card'
															   from dual
															   union all
															   select 'ACC-0001',
																	  'Compact Mechanical Keyboard',
																	  'Accessories',
																	  119.00,
																	  '75% hot-swappable mechanical keyboard, wireless'
															   from dual
															   union all
															   select 'ACC-0002',
																	  'Precision Wireless Mouse',
																	  'Accessories',
																	  79.00,
																	  'Ergonomic wireless mouse, 8000 DPI sensor'
															   from dual
															   union all
															   select 'ACC-0003',
																	  'USB-C 11-in-1 Dock',
																	  'Accessories',
																	  189.00,
																	  'Dual 4K output, 100 W passthrough, 2.5 GbE'
															   from dual
															   union all
															   select 'ACC-0004',
																	  'GaN 100 W Charger',
																	  'Accessories',
																	  69.00,
																	  '3-port GaN charger, USB-C PD 100 W'
															   from dual)
	   select s.sku, s.name, s.price, s.description, c.id as category_id
	   from seed s
				join category c on c.name = s.category) src
on (tgt.sku = src.sku)
when matched then
	update
	set tgt.name        = src.name,
		tgt.description = src.description,
		tgt.price       = src.price,
		tgt.category_id = src.category_id
when not matched then
	insert (name, description, sku, price, category_id)
	values (src.name, src.description, src.sku, src.price, src.category_id);