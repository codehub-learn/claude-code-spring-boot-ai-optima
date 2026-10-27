-- Baseline courier seed: one named courier per depot (CR-0001..CR-0012), so every depot can dispatch from the first boot.
-- The generated couriers CR-0101.. come from R__910_bulk_courier.sql. Idempotent: one MERGE keyed on employee_code.

merge into courier tgt
using (select s.*, d.id as depot_id
       from (select 'CR-0001'          as employee_code,
                    'ATH'              as depot_code,
                    'Giorgos'          as first_name,
                    'Papadopoulos'     as last_name,
                    '+30 694 123 4501' as phone,
                    'VAN'              as vehicle_type,
                    90                 as daily_capacity,
                    date '2014-03-03'  as hired_on
             from dual
             union all
             select 'CR-0002',
                    'THE',
                    'Dimitris',
                    'Nikolaidis',
                    '+30 697 245 3302',
                    'VAN',
                    85,
                    date '2015-06-15'
             from dual
             union all
             select 'CR-0003',
                    'PAT',
                    'Panagiotis',
                    'Spyropoulos',
                    '+30 693 318 7703',
                    'VAN',
                    80,
                    date '2016-01-11'
             from dual
             union all
             select 'CR-0004',
                    'HER',
                    'Manolis',
                    'Papadakis',
                    '+30 698 412 6604',
                    'MOTORBIKE',
                    55,
                    date '2016-09-05'
             from dual
             union all
             select 'CR-0005',
                    'LAR',
                    'Kostas',
                    'Lazaridis',
                    '+30 694 529 1805',
                    'VAN',
                    75,
                    date '2017-04-24'
             from dual
             union all
             select 'CR-0006',
                    'IOA',
                    'Thanasis',
                    'Zografos',
                    '+30 697 633 9206',
                    'VAN',
                    70,
                    date '2018-02-12'
             from dual
             union all
             select 'CR-0007',
                    'VOL',
                    'Eleni',
                    'Michailidou',
                    '+30 693 741 5507',
                    'MOTORBIKE',
                    50,
                    date '2018-11-19'
             from dual
             union all
             select 'CR-0008',
                    'KAV',
                    'Vasilis',
                    'Georgiadis',
                    '+30 698 856 2408',
                    'VAN',
                    70,
                    date '2019-05-06'
             from dual
             union all
             select 'CR-0009',
                    'CHA',
                    'Nikos',
                    'Kalogeropoulos',
                    '+30 694 967 8109',
                    'VAN',
                    65,
                    date '2019-10-14'
             from dual
             union all
             select 'CR-0010',
                    'RHO',
                    'Stelios',
                    'Triantafyllou',
                    '+30 697 078 3310',
                    'MOTORBIKE',
                    50,
                    date '2020-03-02'
             from dual
             union all
             select 'CR-0011',
                    'KAL',
                    'Antonis',
                    'Karamanlis',
                    '+30 693 189 4411',
                    'VAN',
                    65,
                    date '2021-01-25'
             from dual
             union all
             select 'CR-0012',
                    'ALX',
                    'Christina',
                    'Anastasiou',
                    '+30 698 290 6512',
                    'MOTORBIKE',
                    45,
                    date '2021-07-12'
             from dual) s
                join depot d on d.code = s.depot_code) src
on (tgt.employee_code = src.employee_code)
when matched then
    update
    set tgt.depot_id       = src.depot_id,
        tgt.first_name     = src.first_name,
        tgt.last_name      = src.last_name,
        tgt.phone          = src.phone,
        tgt.vehicle_type   = src.vehicle_type,
        tgt.daily_capacity = src.daily_capacity,
        tgt.hired_on       = src.hired_on
when not matched then
    insert (depot_id, employee_code, first_name, last_name, phone, vehicle_type, daily_capacity, hired_on)
    values (src.depot_id, src.employee_code, src.first_name, src.last_name, src.phone, src.vehicle_type, src.daily_capacity,
            src.hired_on);
