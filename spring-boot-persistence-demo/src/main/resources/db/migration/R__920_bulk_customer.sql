-- Bulk customers: 7,988 generated customers (CU-00101..CU-08088) on top of the 12 baseline ones, ~8,000 in total, ~10% businesses.
-- Private customers get a Greek (or, for ~10%, international) name, a mobile number in the real 69X range and an email built from
-- the name. Businesses get a unique "<region/place> <trade> <legal form>" name, a valid 9-digit Greek VAT number (AFM check digit),
-- a landline with the real area code of their depot and a role mailbox on their own domain. Addresses pair a street with a real
-- city/postcode, weighted so Athens and Thessaloniki dominate. created_at falls before the shipment window (2024-11-01).
--
-- Deterministic and idempotent: every "random" value is ORA_HASH(n, max, seed) of the row number, and the MERGE is keyed on
-- customer_code. Rows are never deleted here: R__930 owns everything that references customers.

merge into customer tgt
using (with gen as (select level as n
                    from dual connect by level <= 7988),
            first_m as (select rownum as rn, column_value as v
                        from table(sys.odcivarchar2list('Giorgos', 'Dimitris', 'Nikos', 'Konstantinos', 'Ioannis', 'Christos',
                                                        'Panagiotis', 'Vasilis', 'Thanasis', 'Manolis', 'Michalis', 'Stavros',
                                                        'Antonis', 'Alexandros', 'Spyros', 'Petros', 'Lefteris', 'Pavlos', 'Stelios',
                                                        'Giannis', 'Sotiris', 'Haris', 'Apostolos', 'Aris', 'Fotis', 'Grigoris',
                                                        'Kyriakos', 'Lambros', 'Marios', 'Nikolaos', 'Orestis', 'Periklis',
                                                        'Theodoros', 'Tasos', 'Vangelis', 'Andreas', 'Evangelos', 'Kostas',
                                                        'Thodoris', 'Savvas'))),
            first_f as (select rownum as rn, column_value as v
                        from table(sys.odcivarchar2list('Maria', 'Eleni', 'Katerina', 'Georgia', 'Sofia', 'Anna', 'Despina',
                                                        'Christina', 'Ioanna', 'Dimitra', 'Vasiliki', 'Angeliki', 'Paraskevi',
                                                        'Evangelia', 'Konstantina', 'Athina', 'Irini', 'Marina', 'Nikoleta',
                                                        'Stavroula', 'Fotini', 'Zoi', 'Theodora', 'Panagiota', 'Areti', 'Chrysa',
                                                        'Kalliopi', 'Lydia', 'Olga', 'Rania', 'Voula', 'Alexandra'))),
            surname as (select rownum as rn, column_value as v
                        from table(sys.odcivarchar2list('Papadopoulos', 'Papadakis', 'Karagiannis', 'Nikolaidis', 'Dimitriou',
                                                        'Vasileiou', 'Georgiadis', 'Konstantinidis', 'Ioannou', 'Alexiou',
                                                        'Antoniou', 'Christodoulou', 'Oikonomou', 'Makris', 'Pappas', 'Vlachos',
                                                        'Angelopoulos', 'Theodorou', 'Papanikolaou', 'Stavrou', 'Lazaridis',
                                                        'Michailidis', 'Kyriakou', 'Panagiotopoulos', 'Mavridis', 'Triantafyllou',
                                                        'Kalogeropoulos', 'Anastasiou', 'Sotiriou', 'Petrakis', 'Katsaros',
                                                        'Manolakis', 'Zografos', 'Karamanlis', 'Spyropoulos', 'Tsakiris', 'Farmakis',
                                                        'Vasilakis', 'Leventis', 'Galanis', 'Xenakis', 'Kokkinos', 'Samaras', 'Bakas',
                                                        'Rallis', 'Chatzis', 'Stamatiou', 'Louizos', 'Pantazis', 'Kouros',
                                                        'Sideris', 'Tzanis', 'Daskalakis', 'Fragkos', 'Lykos', 'Moschos',
                                                        'Nomikos', 'Orfanos', 'Paschalis', 'Rigas'))),
            -- international residents and expats: whole names, no surname declension
            intl as (select rownum as rn, column_value as v
                     from table(sys.odcivarchar2list('John Miller', 'Emma Wilson', 'Hans Schneider', 'Anna Mueller', 'Pierre Dubois',
                                                     'Marie Laurent', 'Marco Rossi', 'Giulia Bianchi', 'Carlos Garcia',
                                                     'Lucia Fernandez', 'Jan Kowalski', 'Olga Ivanova', 'Stefan Popescu',
                                                     'Elena Dumitru', 'Mehmet Yilmaz', 'Sarah Cohen', 'James Anderson',
                                                     'Sofie Jensen', 'Lars Nilsson', 'Tomas Novak', 'David Brown', 'Laura Martin',
                                                     'Peter Hoffmann', 'Katja Weber', 'Luca Romano', 'Chiara Conti',
                                                     'Pavel Horak', 'Ana Silva', 'Michael Green', 'Julia Schmidt'))),
            -- company name building blocks: 30 x 30 = 900 distinct "place trade" pairs, enough for every business
            co_a as (select rownum as rn, column_value as v
                     from table(sys.odcivarchar2list('Aegean', 'Attica', 'Hellenic', 'Ionian', 'Macedonian', 'Cretan', 'Peloponnesian',
                                                     'Thessalian', 'Epirote', 'Olympian', 'Pindos', 'Meteora', 'Cycladic',
                                                     'Dodecanese', 'Thracian', 'Arcadian', 'Messinian', 'Laconian', 'Argolic',
                                                     'Boeotian', 'Euboean', 'Corfiot', 'Samian', 'Lesvian', 'Chian', 'Pelion',
                                                     'Rodopi', 'Axios', 'Strymon', 'Evros'))),
            co_b as (select rownum as rn, column_value as v
                     from table(sys.odcivarchar2list('Organic Foods', 'Electronics', 'Pharmacy Supplies', 'Fashion House',
                                                     'Auto Parts', 'Office Supplies', 'Olive Oil', 'Cosmetics', 'Book Distribution',
                                                     'Sports Equipment', 'Wine Cellars', 'Furniture', 'Hardware', 'Toys',
                                                     'Optical Goods', 'Pet Supplies', 'Jewellery', 'Dairy Products',
                                                     'Bakery Wholesale', 'Medical Equipment', 'Tiles and Marble', 'Garden Centre',
                                                     'Coffee Roasters', 'Honey Producers', 'Stationery', 'Lighting',
                                                     'Textiles', 'Bicycle Parts', 'Print Studio', 'Herbal Products'))),
            legal as (select rownum as rn, column_value as v
                      from table(sys.odcivarchar2list('S.A.', 'Ltd', 'I.K.E.'))),
            street as (select rownum as rn, column_value as v
                       from table(sys.odcivarchar2list('Ermou', 'Patission', 'Stadiou', 'Panepistimiou', 'Akadimias', 'Mitropoleos',
                                                       'Tsimiski', 'Egnatia', 'Agiou Dimitriou', 'Aristotelous', 'Venizelou',
                                                       'Kountouriotou', '25is Martiou', 'Ethnikis Antistaseos', 'Dimokratias',
                                                       'Papanastasiou', 'Kolokotroni', 'Riga Fereou', 'Kyprou', 'Amalias', 'Syngrou',
                                                       'Alexandras', 'Mesogeion', 'Thiseos', 'Leoforos Kifisias', 'Vasilissis Sofias',
                                                       'Solonos', 'Pireos', 'Iera Odos', 'Kifisou', 'Marathonos', 'Tritis Septemvriou',
                                                       'Skoufa', 'Ippokratous', 'Charilaou Trikoupi', 'Kapodistriou', 'Ioanninon',
                                                       'Korinthou', 'Maizonos', 'Kanari', 'Korai', 'Dikaiosynis', 'Kalokairinou',
                                                       'Dimitriados'))),
            domain as (select 'gmail.com' as v, 0 as lo, 45 as hi
                       from dual
                       union all
                       select 'yahoo.gr', 45, 60
                       from dual
                       union all
                       select 'outlook.com', 60, 70
                       from dual
                       union all
                       select 'hotmail.com', 70, 80
                       from dual
                       union all
                       select 'otenet.gr', 80, 88
                       from dual
                       union all
                       select 'icloud.com', 88, 94
                       from dual
                       union all
                       select 'yahoo.com', 94, 97
                       from dual
                       union all
                       select 'protonmail.com', 97, 100
                       from dual),
            -- real cities/postcodes with the serving depot, the landline area code and a pick weight; lo/hi are cumulative bounds
            loc as (select city, postal_code, depot_code, area, sum(w) over (order by ord) - w as lo, sum(w) over (order by ord) as hi
                    from (select 1 as ord, 'Athina' as city, '11521' as postal_code, 'ATH' as depot_code, '210' as area, 6 as w
                          from dual
                          union all
                          select 2, 'Athina', '10678', 'ATH', '210', 3
                          from dual
                          union all
                          select 3, 'Athina', '11364', 'ATH', '210', 2
                          from dual
                          union all
                          select 4, 'Kifisia', '14562', 'ATH', '210', 4
                          from dual
                          union all
                          select 5, 'Marousi', '15124', 'ATH', '210', 4
                          from dual
                          union all
                          select 6, 'Glyfada', '16674', 'ATH', '210', 4
                          from dual
                          union all
                          select 7, 'Peiraias', '18531', 'ATH', '210', 4
                          from dual
                          union all
                          select 8, 'Peiraias', '18534', 'ATH', '210', 2
                          from dual
                          union all
                          select 9, 'Peristeri', '12132', 'ATH', '210', 4
                          from dual
                          union all
                          select 10, 'Nea Smyrni', '17121', 'ATH', '210', 3
                          from dual
                          union all
                          select 11, 'Chalandri', '15231', 'ATH', '210', 3
                          from dual
                          union all
                          select 12, 'Agia Paraskevi', '15342', 'ATH', '210', 2
                          from dual
                          union all
                          select 13, 'Ilion', '13122', 'ATH', '210', 2
                          from dual
                          union all
                          select 14, 'Nikaia', '18454', 'ATH', '210', 2
                          from dual
                          union all
                          select 15, 'Kallithea', '17671', 'ATH', '210', 3
                          from dual
                          union all
                          select 16, 'Vyronas', '16231', 'ATH', '210', 2
                          from dual
                          union all
                          select 17, 'Pallini', '15351', 'ATH', '210', 2
                          from dual
                          union all
                          select 18, 'Acharnes', '13671', 'ATH', '210', 2
                          from dual
                          union all
                          select 19, 'Ilioupoli', '16342', 'ATH', '210', 2
                          from dual
                          union all
                          select 20, 'Thessaloniki', '54622', 'THE', '2310', 5
                          from dual
                          union all
                          select 21, 'Thessaloniki', '54248', 'THE', '2310', 3
                          from dual
                          union all
                          select 22, 'Kalamaria', '55131', 'THE', '2310', 3
                          from dual
                          union all
                          select 23, 'Evosmos', '56224', 'THE', '2310', 2
                          from dual
                          union all
                          select 24, 'Pylaia', '55535', 'THE', '2310', 2
                          from dual
                          union all
                          select 25, 'Panorama', '55236', 'THE', '2310', 1
                          from dual
                          union all
                          select 26, 'Patra', '26221', 'PAT', '2610', 3
                          from dual
                          union all
                          select 27, 'Patra', '26441', 'PAT', '2610', 1
                          from dual
                          union all
                          select 28, 'Aigio', '25100', 'PAT', '26910', 1
                          from dual
                          union all
                          select 29, 'Irakleio', '71202', 'HER', '2810', 3
                          from dual
                          union all
                          select 30, 'Irakleio', '71305', 'HER', '2810', 1
                          from dual
                          union all
                          select 31, 'Chersonisos', '70014', 'HER', '28970', 1
                          from dual
                          union all
                          select 32, 'Larisa', '41222', 'LAR', '2410', 3
                          from dual
                          union all
                          select 33, 'Ioannina', '45221', 'IOA', '26510', 2
                          from dual
                          union all
                          select 34, 'Volos', '38221', 'VOL', '24210', 3
                          from dual
                          union all
                          select 35, 'Kavala', '65302', 'KAV', '2510', 2
                          from dual
                          union all
                          select 36, 'Chania', '73134', 'CHA', '28210', 2
                          from dual
                          union all
                          select 37, 'Rodos', '85100', 'RHO', '22410', 2
                          from dual
                          union all
                          select 38, 'Kalamata', '24100', 'KAL', '27210', 2
                          from dual
                          union all
                          select 39, 'Alexandroupoli', '68100', 'ALX', '25510', 1
                          from dual)),
            -- step 1: kind of customer, and the picks that do not depend on it
            base as (select n,
                            case when ora_hash(n, 999, 31) < 105 then 'BUSINESS' else 'PRIVATE' end as customer_type,
                            ora_hash(n, 99, 32)                                                     as intl_roll,
                            ora_hash(n, 99, 33)                                                     as gender_roll,
                            ora_hash(n, 99, 34)                                                     as tier_roll,
                            ora_hash(n, 99, 38)                                                     as domain_roll
                     from gen),
            numbered as (select b.*,
                                row_number() over (partition by customer_type order by n) as type_rn
                         from base b),
            -- step 2: identity (name / company), the locality and the VAT base
            ident as (select nu.n,
                             nu.customer_type,
                             nu.type_rn,
                             nu.tier_roll,
                             nu.domain_roll,
                             case
                                 when nu.customer_type = 'PRIVATE' and nu.intl_roll < 10
                                     then regexp_substr(i.v, '^[^ ]+')
                                 when nu.customer_type = 'PRIVATE' and nu.gender_roll < 50
                                     then ff.v
                                 when nu.customer_type = 'PRIVATE' then fm.v
                                 end                                          as first_name,
                             case
                                 when nu.customer_type = 'PRIVATE' and nu.intl_roll < 10
                                     then regexp_substr(i.v, '[^ ]+$')
                                 when nu.customer_type = 'PRIVATE' and nu.gender_roll < 50 and sn.v like '%os'
                                     then substr(sn.v, 1, length(sn.v) - 2) || 'ou'
                                 when nu.customer_type = 'PRIVATE' and nu.gender_roll < 50 and (sn.v like '%is' or sn.v like '%as')
                                     then substr(sn.v, 1, length(sn.v) - 1)
                                 when nu.customer_type = 'PRIVATE' then sn.v
                                 end                                          as last_name,
                             ca.v                                             as co_place,
                             cb.v                                             as co_trade,
                             lg.v                                             as co_legal,
                             lc.city,
                             lc.postal_code,
                             lc.depot_code,
                             lc.area,
                             st.v || ' ' || (1 + ora_hash(nu.n, 180, 44))     as line1,
                             to_char(10000000 + ora_hash(nu.n, 89999999, 37)) as vat_base
                      from numbered nu
                               left join intl i on i.rn = 1 + ora_hash(nu.n, 29, 40)
                               left join first_m fm on fm.rn = 1 + ora_hash(nu.n, 39, 41)
                               left join first_f ff on ff.rn = 1 + ora_hash(nu.n, 31, 42)
                               left join surname sn on sn.rn = 1 + ora_hash(nu.n, 59, 43)
                               left join co_a ca on ca.rn = 1 + mod(mod(nu.type_rn * 7919, 900), 30)
                               left join co_b cb on cb.rn = 1 + floor(mod(nu.type_rn * 7919, 900) / 30)
                               left join legal lg on lg.rn = 1 + ora_hash(nu.n, 2, 36)
                               join loc lc on ora_hash(nu.n, (select max(hi) from loc) - 1, 45) >= lc.lo
                          and ora_hash(nu.n, (select max(hi) from loc) - 1, 45) < lc.hi
                               join street st on st.rn = 1 + ora_hash(nu.n, 43, 46)),
            -- step 3: email local part, phone digits and the VAT check digit
            detail as (select id.*,
                              case
                                  when customer_type = 'PRIVATE' then lower(first_name || '.' || last_name)
                                  else (case ora_hash(n, 4, 47)
                                            when 0 then 'info'
                                            when 1 then 'orders'
                                            when 2 then 'sales'
                                            when 3 then 'office'
                                            else 'contact' end)
                                  end                                                                                               as local_part,
                              case
                                  when customer_type = 'BUSINESS'
                                      then lower(co_place || regexp_replace(co_trade, '[^A-Za-z]', ''))
                                      || case when ora_hash(n, 4, 48) = 0 then '.com' else '.gr' end
                                  end                                                                                               as business_domain,
                              case
                                  when customer_type = 'PRIVATE'
                                      then '+30 69' || ora_hash(n, 9, 49) || ' ' || lpad(ora_hash(n, 999, 50), 3, '0') || ' '
                                      || lpad(ora_hash(n, 9999, 51), 4, '0')
                                  end                                                                                               as mobile,
                              -- landline digits after the area code: 10 digits in total
                              to_char(power(10, 9 - length(area)) +
                                      ora_hash(n, 9 * power(10, 9 - length(area)) - 1, 52))                                         as landline,
                              mod(mod(to_number(substr(vat_base, 1, 1)) * 256 + to_number(substr(vat_base, 2, 1)) * 128
                                          + to_number(substr(vat_base, 3, 1)) * 64 + to_number(substr(vat_base, 4, 1)) * 32
                                          + to_number(substr(vat_base, 5, 1)) * 16 + to_number(substr(vat_base, 6, 1)) * 8
                                          + to_number(substr(vat_base, 7, 1)) * 4 + to_number(substr(vat_base, 8, 1)) * 2, 11),
                                  10)                                                                                               as vat_check
                       from ident id),
            -- step 4: assemble. email0 is the preferred address; duplicates and baseline clashes get the row number appended
            assembled as (select d.n,
                                 'CU-' || lpad(100 + d.n, 5, '0')                                                                     as customer_code,
                                 d.customer_type,
                                 d.first_name,
                                 d.last_name,
                                 case
                                     when d.customer_type = 'BUSINESS'
                                         then d.co_place || ' ' || d.co_trade || ' ' || d.co_legal end                                as company_name,
                                 case when d.customer_type = 'BUSINESS' then d.vat_base || d.vat_check end                            as vat_number,
                                 case
                                     when d.customer_type = 'BUSINESS' and d.tier_roll < 15 then 'GOLD'
                                     when d.customer_type = 'BUSINESS' and d.tier_roll < 50 then 'SILVER'
                                     else 'STANDARD'
                                     end                                                                                              as contract_tier,
                                 d.local_part
                                     || case
                                            when d.customer_type = 'PRIVATE' and ora_hash(d.n, 99, 53) < 35
                                                then to_char(10 + ora_hash(d.n, 89, 54))
                                     end
                                     || '@'
                                     || case
                                            when d.customer_type = 'PRIVATE' then dm.v
                                            else d.business_domain
                                     end                                                                                              as email0,
                                 case
                                     when d.customer_type = 'PRIVATE' then d.mobile
                                     when length(d.landline) = 7 then '+30 ' || d.area || ' ' || substr(d.landline, 1, 3) || ' ' ||
                                                                      substr(d.landline, 4)
                                     when length(d.landline) = 6 then '+30 ' || d.area || ' ' || substr(d.landline, 1, 3) || ' ' ||
                                                                      substr(d.landline, 4)
                                     else '+30 ' || d.area || ' ' || d.landline
                                     end                                                                                              as phone,
                                 d.depot_code,
                                 d.line1,
                                 d.city,
                                 d.postal_code,
                                 from_tz(cast(timestamp '2018-01-02 08:00:00' + numtodsinterval(ora_hash(d.n, 2480, 55), 'DAY')
                                     + numtodsinterval(ora_hash(d.n, 46799, 56), 'SECOND') as timestamp),
                                         'Europe/Athens')                                                                             as created_at
                          from detail d
                                   join domain dm on d.domain_roll >= dm.lo and d.domain_roll < dm.hi),
            deduped as (select a.*,
                               case
                                   when row_number() over (partition by a.email0 order by a.n) > 1
                                       or a.email0 in (select email from customer where customer_code < 'CU-00100')
                                       then replace(a.email0, '@', a.n || '@')
                                   else a.email0
                                   end as email
                        from assembled a)
       select dd.customer_code,
              dd.customer_type,
              dd.first_name,
              dd.last_name,
              dd.company_name,
              dd.vat_number,
              dd.contract_tier,
              dd.email,
              dd.phone,
              d.id as depot_id,
              dd.line1,
              dd.city,
              dd.postal_code,
              dd.created_at
       from deduped dd
                join depot d on d.code = dd.depot_code) src
on (tgt.customer_code = src.customer_code)
when matched then
    update
    set tgt.customer_type = src.customer_type,
        tgt.first_name    = src.first_name,
        tgt.last_name     = src.last_name,
        tgt.company_name  = src.company_name,
        tgt.vat_number    = src.vat_number,
        tgt.contract_tier = src.contract_tier,
        tgt.email         = src.email,
        tgt.phone         = src.phone,
        tgt.depot_id      = src.depot_id,
        tgt.line1         = src.line1,
        tgt.city          = src.city,
        tgt.postal_code   = src.postal_code
when not matched then
    insert (customer_code, customer_type, first_name, last_name, company_name, vat_number, contract_tier, email, phone, depot_id, line1,
            city, postal_code, created_at, updated_at)
    values (src.customer_code, src.customer_type, src.first_name, src.last_name, src.company_name, src.vat_number, src.contract_tier,
            src.email, src.phone, src.depot_id, src.line1, src.city, src.postal_code, src.created_at, src.created_at);
