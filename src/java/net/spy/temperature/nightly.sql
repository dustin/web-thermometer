-- Doing nightly rollups
insert into rollups_day
  select
  	sensor_id,
		min(sample) as min_reading,
		avg(sample) as avg_reading,
		stddev(sample) as stddev_reading,
		max(sample) as max_reading,
		date(ts) as day
  from
    samples
  where
  	ts >= (select max(day)+1 from rollups_day)
	and ts < current_date
  group by
  	sensor_id, day
;
