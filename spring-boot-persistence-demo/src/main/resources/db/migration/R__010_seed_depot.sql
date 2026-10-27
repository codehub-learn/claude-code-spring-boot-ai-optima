-- Baseline depot seed: the 12 Dromeas depots (2 hubs, 10 local depots) with real Greek cities and postcodes.
-- REPEATABLE migration: re-run whenever this file changes. Idempotent: one MERGE keyed on the natural key (depot.code).
-- The bulk scripts (R__9xx) look depots up by these codes, so the codes are part of the contract:
-- ATH THE PAT HER LAR IOA VOL KAV CHA RHO KAL ALX.

merge into depot tgt
using (select 'ATH'                                  as code,
              'Athens Central Hub'                   as name,
              'HUB'                                  as depot_type,
              'Aspropyrgos'                          as city,
              'Attica'                               as region,
              'Leoforos Athinon 14, Industrial Area' as address_line,
              '19300'                                as postal_code,
              date '2009-03-02'                      as opened_on
       from dual
       union all
       select 'THE',
              'Thessaloniki Central Hub',
              'HUB',
              'Sindos',
              'Central Macedonia',
              'Viomichaniki Periochi Sindou, Block 22',
              '57022',
              date '2010-09-13'
       from dual
       union all
       select 'PAT',
              'Patras Depot',
              'LOCAL',
              'Patra',
              'Western Greece',
              'Georgiou Papandreou 41, Industrial Zone',
              '26504',
              date '2012-05-21'
       from dual
       union all
       select 'HER',
              'Heraklion Depot',
              'LOCAL',
              'Irakleio',
              'Crete',
              'Leoforos Ikarou 20, Industrial Zone',
              '71601',
              date '2013-02-11'
       from dual
       union all
       select 'LAR',
              'Larissa Depot',
              'LOCAL',
              'Larisa',
              'Thessaly',
              '5th km Larissas-Athinon, Industrial Area',
              '41500',
              date '2014-09-01'
       from dual
       union all
       select 'IOA',
              'Ioannina Depot',
              'LOCAL',
              'Ioannina',
              'Epirus',
              'Viotechniki Periochi Ioanninon, Street 4',
              '45500',
              date '2015-04-13'
       from dual
       union all
       select 'VOL',
              'Volos Depot',
              'LOCAL',
              'Volos',
              'Thessaly',
              'Viotechniki Periochi Volou, Street 3',
              '38500',
              date '2015-10-05'
       from dual
       union all
       select 'KAV',
              'Kavala Depot',
              'LOCAL',
              'Kavala',
              'East Macedonia and Thrace',
              'Viomichaniki Periochi Kavalas, Street 7',
              '65404',
              date '2016-03-14'
       from dual
       union all
       select 'CHA',
              'Chania Depot',
              'LOCAL',
              'Souda',
              'Crete',
              'Viomichaniki Periochi Soudas, Street 2',
              '73200',
              date '2017-06-19'
       from dual
       union all
       select 'RHO',
              'Rhodes Depot',
              'LOCAL',
              'Kremasti',
              'South Aegean',
              'Viomichaniki Zoni Kremastis, Street 5',
              '85105',
              date '2018-05-07'
       from dual
       union all
       select 'KAL',
              'Kalamata Depot',
              'LOCAL',
              'Kalamata',
              'Peloponnese',
              'Nedontos 15, Industrial Area',
              '24100',
              date '2019-09-23'
       from dual
       union all
       select 'ALX',
              'Alexandroupoli Depot',
              'LOCAL',
              'Alexandroupoli',
              'East Macedonia and Thrace',
              'Viomichaniki Periochi, Street 6',
              '68100',
              date '2020-02-17'
       from dual) src
on (tgt.code = src.code)
when matched then
    update
    set tgt.name         = src.name,
        tgt.depot_type   = src.depot_type,
        tgt.city         = src.city,
        tgt.region       = src.region,
        tgt.address_line = src.address_line,
        tgt.postal_code  = src.postal_code,
        tgt.opened_on    = src.opened_on
when not matched then
    insert (code, name, depot_type, city, region, address_line, postal_code, opened_on)
    values (src.code, src.name, src.depot_type, src.city, src.region, src.address_line, src.postal_code, src.opened_on);
