-- Baseline customer seed: six business and six private customers (CU-00001..CU-00012), spread over several depots, with valid
-- 9-digit Greek VAT numbers (AFM check digit) for the businesses. The generated customers CU-00101.. come from
-- R__920_bulk_customer.sql. Idempotent: one MERGE keyed on customer_code.

merge into customer tgt
using (select s.*, d.id as depot_id
       from (select 'CU-00001'                        as customer_code,
                    'BUSINESS'                        as customer_type,
                    cast(null as varchar2(100))       as first_name,
                    cast(null as varchar2(100))       as last_name,
                    'Thermaikos Organic Foods S.A.'   as company_name,
                    '094128738'                       as vat_number,
                    'GOLD'                            as contract_tier,
                    'info@thermaikos-organicfoods.gr' as email,
                    '+30 2310 267 410'                as phone,
                    'THE'                             as depot_code,
                    'Tsimiski 87'                     as line1,
                    'Thessaloniki'                    as city,
                    '54622'                           as postal_code,
                    timestamp '2019-02-11 10:15:00'   as created_at
             from dual
             union all
             select 'CU-00002',
                    'BUSINESS',
                    null,
                    null,
                    'Parnassos Pharmacy Supplies I.K.E.',
                    '099876545',
                    'SILVER',
                    'orders@parnassos-pharmacysupplies.gr',
                    '+30 210 612 4480',
                    'ATH',
                    'Leoforos Kifisias 220',
                    'Marousi',
                    '15124',
                    timestamp '2019-06-03 09:40:00'
             from dual
             union all
             select 'CU-00003',
                    'BUSINESS',
                    null,
                    null,
                    'Saronic Marine Parts Ltd',
                    '994012852',
                    'STANDARD',
                    'sales@saronic-marineparts.gr',
                    '+30 210 417 9035',
                    'ATH',
                    'Akti Miaouli 51',
                    'Peiraias',
                    '18535',
                    timestamp '2020-01-20 12:05:00'
             from dual
             union all
             select 'CU-00004',
                    'BUSINESS',
                    null,
                    null,
                    'Taygetos Olive Oil Cooperative',
                    '091234566',
                    'GOLD',
                    'export@taygetos-oliveoil.gr',
                    '+30 27210 21458',
                    'KAL',
                    'Artemidos 12',
                    'Kalamata',
                    '24100',
                    timestamp '2019-09-16 11:30:00'
             from dual
             union all
             select 'CU-00005',
                    'BUSINESS',
                    null,
                    null,
                    'Korinthian Wine Cellars S.A.',
                    '140238673',
                    'SILVER',
                    'info@korinthian-winecellars.gr',
                    '+30 2610 330 782',
                    'PAT',
                    'Korinthou 210',
                    'Patra',
                    '26221',
                    timestamp '2020-05-04 14:20:00'
             from dual
             union all
             select 'CU-00006',
                    'BUSINESS',
                    null,
                    null,
                    'Mykonian Design Studio Ltd',
                    '801234098',
                    'STANDARD',
                    'hello@mykonian-designstudio.gr',
                    '+30 2810 244 190',
                    'HER',
                    'Dikaiosynis 42',
                    'Irakleio',
                    '71202',
                    timestamp '2021-03-08 16:45:00'
             from dual
             union all
             select 'CU-00007',
                    'PRIVATE',
                    'Eleni',
                    'Papadopoulou',
                    null,
                    null,
                    'STANDARD',
                    'eleni.papadopoulou@gmail.com',
                    '+30 694 551 2087',
                    'ATH',
                    'Solonos 34',
                    'Athina',
                    '10673',
                    timestamp '2021-10-12 18:10:00'
             from dual
             union all
             select 'CU-00008',
                    'PRIVATE',
                    'Nikos',
                    'Karagiannis',
                    null,
                    null,
                    'STANDARD',
                    'nikos.karagiannis@yahoo.gr',
                    '+30 697 318 6642',
                    'THE',
                    'Egnatia 112',
                    'Thessaloniki',
                    '54622',
                    timestamp '2022-01-25 08:55:00'
             from dual
             union all
             select 'CU-00009',
                    'PRIVATE',
                    'Maria',
                    'Georgiou',
                    null,
                    null,
                    'STANDARD',
                    'maria.georgiou@outlook.com',
                    '+30 693 702 4419',
                    'PAT',
                    'Maizonos 60',
                    'Patra',
                    '26221',
                    timestamp '2022-06-14 13:35:00'
             from dual
             union all
             select 'CU-00010',
                    'PRIVATE',
                    'Dimitris',
                    'Makris',
                    null,
                    null,
                    'STANDARD',
                    'dimitris.makris@gmail.com',
                    '+30 698 240 9135',
                    'LAR',
                    'Papanastasiou 25',
                    'Larisa',
                    '41222',
                    timestamp '2023-02-02 19:20:00'
             from dual
             union all
             select 'CU-00011',
                    'PRIVATE',
                    'Sofia',
                    'Vlachou',
                    null,
                    null,
                    'STANDARD',
                    'sofia.vlachou@hotmail.com',
                    '+30 694 876 3320',
                    'HER',
                    'Kalokairinou 18',
                    'Irakleio',
                    '71202',
                    timestamp '2023-07-19 10:00:00'
             from dual
             union all
             select 'CU-00012',
                    'PRIVATE',
                    'Giannis',
                    'Oikonomou',
                    null,
                    null,
                    'STANDARD',
                    'giannis.oikonomou@gmail.com',
                    '+30 697 465 1078',
                    'VOL',
                    'Dimitriados 77',
                    'Volos',
                    '38221',
                    timestamp '2024-04-09 17:25:00'
             from dual) s
                join depot d on d.code = s.depot_code) src
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
            city, postal_code, created_at)
    values (src.customer_code, src.customer_type, src.first_name, src.last_name, src.company_name, src.vat_number, src.contract_tier,
            src.email, src.phone, src.depot_id, src.line1, src.city, src.postal_code, from_tz(src.created_at, 'Europe/Athens'));
