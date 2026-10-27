-- Bulk shipments, tracking events and invoices: ~120,000 shipments over 2024-11-01 .. 2026-10-27 (the "as of" moment is
-- 2026-10-27 14:30 Europe/Athens), their scan events and the monthly invoices of the business customers.
--
-- Everything the three tables hold is derived from a coherent lifecycle, not from independent random columns:
--   * volume per day follows the week (Mon-Fri busy, Saturday light, Sunday near-empty), the season (Nov/Dec peak, August dip) and
--     a gentle growth trend; ~70% of shipments come from business customers, skewed so a few accounts send most of the volume
--   * service level: SAME_DAY (only inside one depot area, booked 08:00-14:00), EXPRESS or STANDARD; price = base + per-kg + zone
--     surcharge (island legs cost more), minus the sender's contract discount
--   * a shipment is picked up the same day if booked before 15:00 (otherwise next business day, Mon-Sat), scanned into the
--     destination depot overnight when it crosses depots, goes out for delivery the next morning, and is delivered in working hours
--   * ~6% are delayed by 1-2 business days, ~4% fail the first attempt and are retried the next business day (~15% of those fail
--     again and stay FAILED_ATTEMPT); shipments booked in the last hours simply have not progressed yet
--   * the delivering courier is a courier of the destination depot, assigned when the shipment goes out for delivery
--   * every delivered shipment of a business customer belongs to exactly one monthly invoice (delivery month, closed months only)
--
-- Deterministic: every "random" value is ORA_HASH of the shipment's tracking number (or the row number), never DBMS_RANDOM.
-- Not incremental: this script OWNS all shipment / tracking_event / invoice rows. A re-run wipes them (children first) and
-- regenerates the same data. Business days here are Monday to Saturday (couriers do not deliver on Sunday).
-- Business-day arithmetic used throughout: for a Mon-Sat date x and k business days,
--   trunc(x, 'IW') + floor(((x - trunc(x, 'IW')) + k) / 6) * 7 + mod((x - trunc(x, 'IW')) + k, 6)

delete
from tracking_event;

delete
from shipment;

delete
from invoice;

-- Step 1: shipments, as booked. Status stays CREATED and the delivery columns stay empty until step 4 derives them from the events.
insert into shipment (tracking_number, customer_id, origin_depot_id, destination_depot_id, service_level, weight_kg, price,
                      recipient_name, recipient_line1, recipient_city, recipient_postal_code, picked_up_at, promised_by, created_at,
                      updated_at)
with days as (select d, round(120000 * w / sum(w) over ()) as cnt
              from (select d,
                           case d - trunc(d, 'IW') when 5 then 4 when 6 then 1 else 10 end
                               * case extract(month from d) when 11 then 1.5 when 12 then 1.5 when 8 then 0.6 when 1 then 0.8 else 1 end
                               * (0.7 + 0.6 * (d - date '2024-11-01') / 726) as w
                    from (select date '2024-11-01' + level - 1 as d
                          from dual connect by level <= 727))),
     slots as (select d.d, g.k
               from days d
                        join (select level as k from dual connect by level <= 1000) g on g.k <= d.cnt),
     numbered as (select d, row_number() over (order by d, k) as n
                  from slots),
     s1 as (select d,
                   n,
                   100000000 + mod(n * 48271, 899999999) as serial
            from numbered),
     s2 as (select d,
                   n,
                   'DR' || serial || mod(serial, 7)                                                                     as tn,
                   case when ora_hash('DR' || serial || mod(serial, 7), 99, 61) < 70 then 'BUSINESS' else 'PRIVATE' end as ctype
            from s1),
     -- the sender's rank inside its type, skewed towards the first accounts (business 1.6, private 1.3) so a few accounts send most
     -- of the volume. Computed here, not in the join, so the join below is a plain equi-join
     s2b as (select s.*,
                    1 + floor(case ctype
                                  when 'BUSINESS' then (select count(*) from customer where customer_type = 'BUSINESS')
                                  else (select count(*) from customer where customer_type = 'PRIVATE')
                                  end *
                              power(ora_hash(tn, 999999, 62) / 1000000, case ctype when 'BUSINESS' then 1.6 else 1.3 end)) as pick_rn
             from s2 s),
     street as (select rownum as rn, column_value as v
                from table(sys.odcivarchar2list('Ermou', 'Patission', 'Stadiou', 'Panepistimiou', 'Akadimias', 'Mitropoleos', 'Tsimiski',
                                                'Egnatia', 'Agiou Dimitriou', 'Aristotelous', 'Venizelou', 'Kountouriotou', '25is Martiou',
                                                'Ethnikis Antistaseos', 'Dimokratias', 'Papanastasiou', 'Kolokotroni', 'Riga Fereou',
                                                'Kyprou',
                                                'Amalias', 'Syngrou', 'Alexandras', 'Mesogeion', 'Thiseos', 'Leoforos Kifisias',
                                                'Vasilissis Sofias', 'Solonos', 'Pireos', 'Iera Odos', 'Kifisou', 'Marathonos',
                                                'Tritis Septemvriou', 'Skoufa', 'Ippokratous', 'Charilaou Trikoupi', 'Kapodistriou',
                                                'Ioanninon', 'Korinthou', 'Maizonos', 'Kanari', 'Korai', 'Dikaiosynis', 'Kalokairinou',
                                                'Dimitriados'))),
     -- recipient localities: same weighted city/postcode/depot pool as R__920_bulk_customer.sql (without the phone area code)
     loc as (select city, postal_code, depot_code, sum(w) over (order by ord) - w as lo, sum(w) over (order by ord) as hi
             from (select 1 as ord, 'Athina' as city, '11521' as postal_code, 'ATH' as depot_code, 6 as w
                   from dual
                   union all
                   select 2, 'Athina', '10678', 'ATH', 3
                   from dual
                   union all
                   select 3, 'Athina', '11364', 'ATH', 2
                   from dual
                   union all
                   select 4, 'Kifisia', '14562', 'ATH', 4
                   from dual
                   union all
                   select 5, 'Marousi', '15124', 'ATH', 4
                   from dual
                   union all
                   select 6, 'Glyfada', '16674', 'ATH', 4
                   from dual
                   union all
                   select 7, 'Peiraias', '18531', 'ATH', 4
                   from dual
                   union all
                   select 8, 'Peiraias', '18534', 'ATH', 2
                   from dual
                   union all
                   select 9, 'Peristeri', '12132', 'ATH', 4
                   from dual
                   union all
                   select 10, 'Nea Smyrni', '17121', 'ATH', 3
                   from dual
                   union all
                   select 11, 'Chalandri', '15231', 'ATH', 3
                   from dual
                   union all
                   select 12, 'Agia Paraskevi', '15342', 'ATH', 2
                   from dual
                   union all
                   select 13, 'Ilion', '13122', 'ATH', 2
                   from dual
                   union all
                   select 14, 'Nikaia', '18454', 'ATH', 2
                   from dual
                   union all
                   select 15, 'Kallithea', '17671', 'ATH', 3
                   from dual
                   union all
                   select 16, 'Vyronas', '16231', 'ATH', 2
                   from dual
                   union all
                   select 17, 'Pallini', '15351', 'ATH', 2
                   from dual
                   union all
                   select 18, 'Acharnes', '13671', 'ATH', 2
                   from dual
                   union all
                   select 19, 'Ilioupoli', '16342', 'ATH', 2
                   from dual
                   union all
                   select 20, 'Thessaloniki', '54622', 'THE', 5
                   from dual
                   union all
                   select 21, 'Thessaloniki', '54248', 'THE', 3
                   from dual
                   union all
                   select 22, 'Kalamaria', '55131', 'THE', 3
                   from dual
                   union all
                   select 23, 'Evosmos', '56224', 'THE', 2
                   from dual
                   union all
                   select 24, 'Pylaia', '55535', 'THE', 2
                   from dual
                   union all
                   select 25, 'Panorama', '55236', 'THE', 1
                   from dual
                   union all
                   select 26, 'Patra', '26221', 'PAT', 3
                   from dual
                   union all
                   select 27, 'Patra', '26441', 'PAT', 1
                   from dual
                   union all
                   select 28, 'Aigio', '25100', 'PAT', 1
                   from dual
                   union all
                   select 29, 'Irakleio', '71202', 'HER', 3
                   from dual
                   union all
                   select 30, 'Irakleio', '71305', 'HER', 1
                   from dual
                   union all
                   select 31, 'Chersonisos', '70014', 'HER', 1
                   from dual
                   union all
                   select 32, 'Larisa', '41222', 'LAR', 3
                   from dual
                   union all
                   select 33, 'Ioannina', '45221', 'IOA', 2
                   from dual
                   union all
                   select 34, 'Volos', '38221', 'VOL', 3
                   from dual
                   union all
                   select 35, 'Kavala', '65302', 'KAV', 2
                   from dual
                   union all
                   select 36, 'Chania', '73134', 'CHA', 2
                   from dual
                   union all
                   select 37, 'Rodos', '85100', 'RHO', 2
                   from dual
                   union all
                   select 38, 'Kalamata', '24100', 'KAL', 2
                   from dual
                   union all
                   select 39, 'Alexandroupoli', '68100', 'ALX', 1
                   from dual)),
     cust as (select id,
                     customer_type,
                     contract_tier,
                     depot_id,
                     case customer_type when 'BUSINESS' then company_name else first_name || ' ' || last_name end as display_name,
                     row_number() over (partition by customer_type order by id)                                   as rn_t,
                     row_number() over (order by id)                                                              as rn_all
              from customer),
     -- step 2: sender, recipient name and address, origin and destination depots
     s3 as (select s.d,
                   s.n,
                   s.tn,
                   c.id                                                                                            as customer_id,
                   c.contract_tier,
                   od.id                                                                                           as origin_id,
                   od.code                                                                                         as origin_code,
                   dd.id                                                                                           as dest_id,
                   dd.code                                                                                         as dest_code,
                   r.display_name                                                                                  as recipient_name,
                   st.v || ' ' || (1 + ora_hash(s.tn, 180, 70))
                       || case when ora_hash(s.tn, 99, 71) < 20 then ', floor ' || (1 + ora_hash(s.tn, 5, 72)) end as recipient_line1,
                   rl.city                                                                                         as recipient_city,
                   rl.postal_code                                                                                  as recipient_postal_code
            from s2b s
                     join cust c on c.customer_type = s.ctype and c.rn_t = s.pick_rn
                     join depot od on od.id = c.depot_id
                     join loc rl on ora_hash(s.tn, (select max(hi) from loc) - 1, 63) >= rl.lo
                and ora_hash(s.tn, (select max(hi) from loc) - 1, 63) < rl.hi
                     join depot dd on dd.code = rl.depot_code
                     join cust r on r.rn_all = 1 + ora_hash(s.tn, (select count(*) from customer) - 1, 68)
                     join street st on st.rn = 1 + ora_hash(s.tn, 43, 69)),
     -- step 3: service level, weight and the zone flags
     s4 as (select s.*,
                   case
                       when ora_hash(tn, 99, 64) < 8 and origin_id = dest_id and d - trunc(d, 'IW') <> 6 then 'SAME_DAY'
                       when ora_hash(tn, 99, 64) < 30 then 'EXPRESS'
                       else 'STANDARD'
                       end                                                           as svc,
                   d - trunc(d, 'IW')                                                as c_wd,
                   case when origin_id = dest_id then 1 else 0 end                   as same_depot,
                   case
                       when (origin_code in ('HER', 'CHA', 'RHO') or dest_code in ('HER', 'CHA', 'RHO')) and origin_id <> dest_id then 1
                       else 0
                       end                                                           as island,
                   round(0.2 + 30 * power(ora_hash(tn, 999999, 67) / 1000000, 4), 2) as weight_kg
            from s3 s),
     -- booking minute of the day: same-day 08:00-13:59, otherwise 06:00-21:59 (06:00-14:30 on the last day, "now" is 14:30)
     s5 as (select s.*,
                   case
                       when svc = 'SAME_DAY' then 480 + ora_hash(tn, 359, 65)
                       when d = date '2026-10-27' then 360 + ora_hash(tn, 509, 65)
                       else 360 + ora_hash(tn, 959, 65)
                       end as book_min
            from s4 s),
     -- pickup day/minute and lead time in business days
     s6 as (select s.*,
                   case
                       when c_wd = 6 then d + 1
                       when svc = 'SAME_DAY' or book_min < 900 then d
                       else d + case when c_wd = 5 then 2 else 1 end
                       end      as p_day,
                   case
                       when c_wd = 6 or not (svc = 'SAME_DAY' or book_min < 900) then 540 + ora_hash(tn, 420, 73)
                       when svc = 'SAME_DAY' then book_min + 60 + ora_hash(tn, 60, 72)
                       else least(1080, book_min + 60 + ora_hash(tn, 180, 72))
                       end      as p_min,
                   case svc when 'SAME_DAY' then 0 when 'EXPRESS' then 1 else case when same_depot = 1 then 1 else 2 end end
                       + island as lead
            from s5 s),
     s7 as (select s.*,
                   d + (book_min * 60 + ora_hash(tn, 59, 66)) / 86400 as created_ts,
                   p_day + p_min / 1440                               as pickup_ts,
                   trunc(p_day, 'IW') + floor(((p_day - trunc(p_day, 'IW')) + lead) / 6) * 7
                       + mod((p_day - trunc(p_day, 'IW')) + lead, 6)  as ofd_day
            from s6 s)
select tn,
       customer_id,
       origin_id,
       dest_id,
       svc,
       weight_kg,
       -- price = (base + per-kg above the first kilo + zone surcharge) * contract discount
       round((case svc when 'SAME_DAY' then 9.5 when 'EXPRESS' then 5.9 else 3.2 end
           + case svc when 'SAME_DAY' then 1.4 when 'EXPRESS' then 0.95 else 0.6 end * greatest(weight_kg - 1, 0)
           + case when same_depot = 1 then 0 when island = 1 then 4 else 1.5 end)
                 * case contract_tier when 'GOLD' then 0.9 when 'SILVER' then 0.95 else 1 end, 2),
       recipient_name,
       recipient_line1,
       recipient_city,
       recipient_postal_code,
       -- picked up only if that moment has already passed
       case when pickup_ts <= timestamp '2026-10-27 14:30:00' then from_tz(cast(pickup_ts as timestamp), 'Europe/Athens') end,
       from_tz(cast(ofd_day + 20 / 24 as timestamp), 'Europe/Athens'),
       from_tz(cast(created_ts as timestamp), 'Europe/Athens'),
       from_tz(cast(created_ts as timestamp), 'Europe/Athens')
from s7;

-- Step 2: tracking events. The plan is re-derived from the shipment row (pickup, promised day) plus ORA_HASH of the tracking number,
-- so no scratch table is needed. Only events that have already happened ("now" = 2026-10-27 14:30) are inserted.
--   PICKED_UP (origin depot) -> AT_HUB (destination depot, only when it crossed depots, overnight) -> OUT_FOR_DELIVERY ->
--   DELIVERED, or FAILED_ATTEMPT -> OUT_FOR_DELIVERY (next business day) -> DELIVERED / FAILED_ATTEMPT
insert into tracking_event (shipment_id, depot_id, status, occurred_at, note)
with plan as (select s.id,
                     s.tracking_number                                                     as tn,
                     s.service_level                                                       as svc,
                     s.origin_depot_id                                                     as origin_id,
                     s.destination_depot_id                                                as dest_id,
                     cast(s.picked_up_at at time zone 'Europe/Athens' as date)             as pick_ts,
                     trunc(cast(s.promised_by at time zone 'Europe/Athens' as date))       as ofd_day,
                     case
                         when ora_hash(s.tracking_number, 99, 74) < 4 then 1
                         when ora_hash(s.tracking_number, 99, 74) < 6 then 2
                         else 0
                         end                                                               as delay_days,
                     case when ora_hash(s.tracking_number, 999, 78) < 40 then 1 else 0 end as fail1,
                     case when ora_hash(s.tracking_number, 99, 79) < 15 then 1 else 0 end  as term
              from shipment s
              where s.picked_up_at is not null),
     p1 as (select p.*,
                   trunc(ofd_day, 'IW') + floor(((ofd_day - trunc(ofd_day, 'IW')) + delay_days) / 6) * 7
                       + mod((ofd_day - trunc(ofd_day, 'IW')) + delay_days, 6) as ofd1_day
            from plan p),
     p2 as (select p.*,
                   trunc(ofd1_day, 'IW') + floor(((ofd1_day - trunc(ofd1_day, 'IW')) + 1) / 6) * 7
                       + mod((ofd1_day - trunc(ofd1_day, 'IW')) + 1, 6) as ofd2_day
            from p1 p),
     p3 as (select p.*,
                   ofd_day + (60 + ora_hash(tn, 330, 82)) / 1440   as hub_ts,
                   case
                       when svc = 'SAME_DAY' and delay_days = 0 then pick_ts + (40 + ora_hash(tn, 20, 75)) / 1440
                       else ofd1_day + (510 + ora_hash(tn, 60, 76)) / 1440
                       end                                         as ofd1_ts,
                   ofd2_day + (510 + ora_hash(tn, 60, 80)) / 1440  as ofd2_ts,
                   ofd2_day + (600 + ora_hash(tn, 570, 81)) / 1440 as res2_ts
            from p2 p),
     p4 as (select p.*,
                   case
                       when svc = 'SAME_DAY' and delay_days = 0 then ofd1_ts + (60 + ora_hash(tn, 120, 77)) / 1440
                       else ofd1_day + (600 + ora_hash(tn, 570, 77)) / 1440
                       end as res1_ts
            from p3 p)
select id, origin_id, 'PICKED_UP', from_tz(cast(pick_ts as timestamp), 'Europe/Athens'), 'Collected from sender'
from p4
union all
select id, dest_id, 'AT_HUB', from_tz(cast(hub_ts as timestamp), 'Europe/Athens'), 'Arrived at destination depot'
from p4
where origin_id <> dest_id
  and hub_ts <= timestamp '2026-10-27 14:30:00'
union all
select id, dest_id, 'OUT_FOR_DELIVERY', from_tz(cast(ofd1_ts as timestamp), 'Europe/Athens'), 'Out for delivery'
from p4
where ofd1_ts <= timestamp '2026-10-27 14:30:00'
union all
select id,
       dest_id,
       case when fail1 = 1 then 'FAILED_ATTEMPT' else 'DELIVERED' end,
       from_tz(cast(res1_ts as timestamp), 'Europe/Athens'),
       case
           when fail1 = 1 then case ora_hash(tn, 2, 83)
                                   when 0 then 'Recipient not available'
                                   when 1 then 'Address not found'
                                   else 'Premises closed' end
           else case ora_hash(tn, 2, 83)
                    when 0 then 'Delivered to recipient'
                    when 1 then 'Handed to a neighbour'
                    else 'Left at reception desk' end
           end
from p4
where res1_ts <= timestamp '2026-10-27 14:30:00'
union all
select id, dest_id, 'OUT_FOR_DELIVERY', from_tz(cast(ofd2_ts as timestamp), 'Europe/Athens'), 'Out for delivery (second attempt)'
from p4
where fail1 = 1
  and ofd2_ts <= timestamp '2026-10-27 14:30:00'
union all
select id,
       dest_id,
       case when term = 1 then 'FAILED_ATTEMPT' else 'DELIVERED' end,
       from_tz(cast(res2_ts as timestamp), 'Europe/Athens'),
       case
           when term = 1 then 'Second attempt failed, held at depot'
           else case ora_hash(tn, 2, 83)
                    when 0 then 'Delivered to recipient'
                    when 1 then 'Handed to a neighbour'
                    else 'Left at reception desk' end
           end
from p4
where fail1 = 1
  and res2_ts <= timestamp '2026-10-27 14:30:00';

-- Step 3: roll the events up into the shipment row: status = last event, attempts = out-for-delivery runs, delivered_at,
-- updated_at = time of the last event. Shipments without events keep status CREATED.
merge into shipment s
using (select e.shipment_id,
              max(e.status)                                                   keep (dense_rank last order by e.occurred_at)         as last_status, count(case when e.status = 'OUT_FOR_DELIVERY' then 1 end) as attempts,
              max(case when e.status = 'DELIVERED' then e.occurred_at end) as delivered_at,
              max(e.occurred_at)                                           as last_at
       from tracking_event e
       group by e.shipment_id) x
on (s.id = x.shipment_id)
when matched then
    update
    set s.status       = x.last_status,
        s.attempts     = x.attempts,
        s.delivered_at = x.delivered_at,
        s.updated_at   = x.last_at;

-- Step 4: dispatch. A shipment that went out for delivery is assigned to an active courier of its destination depot.
merge into shipment s
using (select sh.id, c.id as courier_id
       from shipment sh
                join (select id,
                             depot_id,
                             row_number() over (partition by depot_id order by id) as rn,
                             count(*) over (partition by depot_id)                 as cnt
                      from courier
                      where active = 1) c on c.depot_id = sh.destination_depot_id
           and c.rn = 1 + mod(ora_hash(sh.tracking_number, 999999, 84), c.cnt)
       where sh.attempts > 0) x
on (s.id = x.id)
when matched then
    update
    set s.courier_id = x.courier_id;

-- Step 5: one invoice per business customer and delivery month, for closed months only (before 2026-10-01). 24% VAT, issued 1-5 days
-- after month end, due in 30 days; ~92% are paid 5-45 days after issue, the rest are OVERDUE (past due) or still ISSUED.
insert into invoice (invoice_number, customer_id, period_start, period_end, issued_on, due_on, net_amount, vat_amount, total_amount,
                     status, paid_on, created_at, updated_at)
with billed as (select s.customer_id,
                       trunc(cast(s.delivered_at at time zone 'Europe/Athens' as date), 'MM') as period_start,
                       sum(s.price)                                                           as net_amount
                from shipment s
                         join customer c on c.id = s.customer_id and c.customer_type = 'BUSINESS'
                where s.status = 'DELIVERED'
                  and s.delivered_at < from_tz(timestamp '2026-10-01 00:00:00', 'Europe/Athens')
                group by s.customer_id, trunc(cast(s.delivered_at at time zone 'Europe/Athens' as date), 'MM')),
     inv as (select b.*,
                    last_day(b.period_start)                                                  as period_end,
                    last_day(b.period_start) + 1
                        + ora_hash(b.customer_id || to_char(b.period_start, 'YYYYMM'), 4, 85) as issued_on,
                    row_number() over (partition by b.period_start order by b.customer_id)    as seq,
                    ora_hash(b.customer_id || to_char(b.period_start, 'YYYYMM'), 99, 86)      as pay_roll,
                    5 + ora_hash(b.customer_id || to_char(b.period_start, 'YYYYMM'), 40, 87)  as pay_days
             from billed b),
     fin as (select i.*,
                    i.issued_on + 30              as due_on,
                    round(i.net_amount * 0.24, 2) as vat_amount,
                    i.issued_on + i.pay_days      as paid_candidate
             from inv i)
select 'INV-' || to_char(period_start, 'YYYYMM') || '-' || lpad(seq, 5, '0'),
       customer_id,
       period_start,
       period_end,
       issued_on,
       due_on,
       net_amount,
       vat_amount,
       net_amount + vat_amount,
       case
           when pay_roll < 92 and paid_candidate <= date '2026-10-27' then 'PAID'
           when due_on < date '2026-10-27' then 'OVERDUE'
           else 'ISSUED'
           end,
       case when pay_roll < 92 and paid_candidate <= date '2026-10-27' then paid_candidate end,
       from_tz(cast(issued_on + 6 / 24 as timestamp), 'Europe/Athens'),
       from_tz(cast(issued_on + 6 / 24 as timestamp), 'Europe/Athens')
from fin;

-- Step 6: link every delivered shipment of a billed customer to its invoice.
merge into shipment s
using (select sh.id, i.id as invoice_id
       from shipment sh
                join invoice i on i.customer_id = sh.customer_id
           and i.period_start = trunc(cast(sh.delivered_at at time zone 'Europe/Athens' as date), 'MM')
       where sh.status = 'DELIVERED') x
on (s.id = x.id)
when matched then
    update
    set s.invoice_id = x.invoice_id;
