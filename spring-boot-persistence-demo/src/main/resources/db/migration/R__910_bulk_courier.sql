-- Bulk couriers: 388 generated couriers (CR-0101..CR-0488) on top of the 12 baseline ones, ~400 in total.
-- Realism comes from curated pools, never random strings: Greek first names and surnames (the female surname form is derived from the
-- male one), a mobile number in the real 69X range, depots weighted by size (Athens and Thessaloniki are the big hubs), a vehicle
-- type that fits the depot (cargo bikes only in the two big cities) and a daily stop capacity that fits the vehicle.
--
-- Deterministic and idempotent: every "random" value is ORA_HASH(n, max, seed) of the row number, so a re-run regenerates identical
-- rows and the MERGE (keyed on employee_code) updates them in place. No DBMS_RANDOM, no PL/SQL.

merge into courier tgt
using (with gen as (select level as n
                    from dual connect by level <= 388),
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
            -- depot weights: share of the courier force per depot; lo/hi are the cumulative bounds used to pick one by hash
            depot_w as (select code, w, sum(w) over (order by ord) - w as lo, sum(w) over (order by ord) as hi
                        from (select 'ATH' as code, 10 as w, 1 as ord
                              from dual
                              union all
                              select 'THE', 6, 2
                              from dual
                              union all
                              select 'PAT', 2, 3
                              from dual
                              union all
                              select 'HER', 2, 4
                              from dual
                              union all
                              select 'LAR', 1, 5
                              from dual
                              union all
                              select 'IOA', 1, 6
                              from dual
                              union all
                              select 'VOL', 1, 7
                              from dual
                              union all
                              select 'KAV', 1, 8
                              from dual
                              union all
                              select 'CHA', 1, 9
                              from dual
                              union all
                              select 'RHO', 1, 10
                              from dual
                              union all
                              select 'KAL', 1, 11
                              from dual
                              union all
                              select 'ALX', 1, 12
                              from dual)),
            base as (select g.n,
                            dw.code                                                    as depot_code,
                            case when ora_hash(g.n, 99, 11) < 18 then 'F' else 'M' end as gender,
                            1 + ora_hash(g.n, (select count(*) from first_m) - 1, 12)  as fm_idx,
                            1 + ora_hash(g.n, (select count(*) from first_f) - 1, 13)  as ff_idx,
                            1 + ora_hash(g.n, (select count(*) from surname) - 1, 14)  as sn_idx,
                            ora_hash(g.n, 99, 16)                                      as vehicle_roll
                     from gen g
                              join depot_w dw on ora_hash(g.n, (select sum(w) from depot_w) - 1, 15) >= dw.lo
                         and ora_hash(g.n, (select sum(w) from depot_w) - 1, 15) < dw.hi),
            named as (select b.n,
                             b.depot_code,
                             b.vehicle_roll,
                             case b.gender when 'F' then ff.v else fm.v end as first_name,
                             case
                                 when b.gender = 'F' and sn.v like '%os' then substr(sn.v, 1, length(sn.v) - 2) || 'ou'
                                 when b.gender = 'F' and (sn.v like '%is' or sn.v like '%as') then substr(sn.v, 1, length(sn.v) - 1)
                                 else sn.v
                                 end                                        as last_name
                      from base b
                               join first_m fm on fm.rn = b.fm_idx
                               join first_f ff on ff.rn = b.ff_idx
                               join surname sn on sn.rn = b.sn_idx),
            typed as (select n,
                             depot_code,
                             first_name,
                             last_name,
                             case
                                 when depot_code in ('ATH', 'THE') and vehicle_roll < 32 then 'MOTORBIKE'
                                 when depot_code in ('ATH', 'THE') and vehicle_roll < 82 then 'VAN'
                                 when depot_code in ('ATH', 'THE') and vehicle_roll < 90 then 'CARGO_BIKE'
                                 when depot_code in ('ATH', 'THE') then 'TRUCK'
                                 when vehicle_roll < 25 then 'MOTORBIKE'
                                 when vehicle_roll < 85 then 'VAN'
                                 else 'TRUCK'
                                 end as vehicle_type
                      from named)
       select 'CR-' || lpad(100 + t.n, 4, '0')                      as employee_code,
              d.id                                                  as depot_id,
              t.first_name,
              t.last_name,
              '+30 69' || ora_hash(t.n, 9, 17) || ' ' || lpad(ora_hash(t.n, 999, 18), 3, '0') || ' '
                  || lpad(ora_hash(t.n, 9999, 19), 4, '0')          as phone,
              t.vehicle_type,
              case t.vehicle_type
                  when 'MOTORBIKE' then 45 + ora_hash(t.n, 25, 20)
                  when 'CARGO_BIKE' then 25 + ora_hash(t.n, 15, 20)
                  when 'VAN' then 60 + ora_hash(t.n, 50, 20)
                  else 90 + ora_hash(t.n, 60, 20)
                  end                                               as daily_capacity,
              case when ora_hash(t.n, 99, 21) < 4 then 0 else 1 end as active,
              date '2012-01-02' + ora_hash(t.n, 5000, 22)           as hired_on
       from typed t
                join depot d on d.code = t.depot_code) src
on (tgt.employee_code = src.employee_code)
when matched then
    update
    set tgt.depot_id       = src.depot_id,
        tgt.first_name     = src.first_name,
        tgt.last_name      = src.last_name,
        tgt.phone          = src.phone,
        tgt.vehicle_type   = src.vehicle_type,
        tgt.daily_capacity = src.daily_capacity,
        tgt.active         = src.active,
        tgt.hired_on       = src.hired_on
when not matched then
    insert (depot_id, employee_code, first_name, last_name, phone, vehicle_type, daily_capacity, active, hired_on)
    values (src.depot_id, src.employee_code, src.first_name, src.last_name, src.phone, src.vehicle_type, src.daily_capacity, src.active,
            src.hired_on);
