package gr.codelearn.springbootai.persistencedemo.shipment;

import gr.codelearn.springbootai.persistencedemo.shipment.dto.DepotLaneVolume;
import gr.codelearn.springbootai.persistencedemo.shipment.dto.ServiceLevelPerformance;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Plain-SQL reporting queries over the shipment tables, run with {@link JdbcClient} and mapped straight onto records. Kept apart from
 * {@link ShipmentRepository} (Spring Data JPA) because these rows are read-only report shapes, not entities.
 */
@Repository
@RequiredArgsConstructor
class ShipmentReportRepository {

	private final JdbcClient jdbcClient;

	/**
	 * The busiest origin to destination depot lanes by shipment count, with their average parcel weight.
	 */
	List<DepotLaneVolume> findBusiestLanes(final int limit) {
		return jdbcClient.sql("""
							  select o.code as origin_depot_code, d.code as destination_depot_code, count(*) as shipments,
							         round(avg(s.weight_kg), 2) as avg_weight_kg
							  from shipment s
							  join depot o on o.id = s.origin_depot_id
							  join depot d on d.id = s.destination_depot_id
							  group by o.code, d.code
							  order by count(*) desc, o.code, d.code
							  fetch first :limit rows only""")
						 .param("limit", limit)
						 // An explicit DataClassRowMapper instead of query(Class): the latter infers a @Nullable element type under
						 // JSpecify and makes list() look like List<@Nullable DepotLaneVolume>. The mapping is the same and rows are
						 // never null.
						 .query(new DataClassRowMapper<>(DepotLaneVolume.class))
						 .list();
	}

	/**
	 * On-time performance per service level for shipments delivered in the period: volume, late count (delivered after {@code
	 * promised_by})
	 * and average hours from pickup to delivery. The enum column is converted to {@link ServiceLevel} by the mapper.
	 */
	List<ServiceLevelPerformance> findServiceLevelPerformance(final OffsetDateTime from, final OffsetDateTime to) {
		return jdbcClient.sql("""
							  select s.service_level, count(*) as delivered,
							         sum(case when s.delivered_at > s.promised_by then 1 else 0 end) as late,
							         round(avg((cast(s.delivered_at as date) - cast(s.picked_up_at as date)) * 24), 1) as avg_transit_hours
							  from shipment s
							  where s.status = 'DELIVERED' and s.delivered_at >= :from and s.delivered_at < :to
							  group by s.service_level
							  order by s.service_level""")
						 .param("from", from)
						 .param("to", to)
						 .query(new DataClassRowMapper<>(ServiceLevelPerformance.class))
						 .list();
	}
}