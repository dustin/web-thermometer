-- Report stuff
--
-- $Id: report.sql,v 1.1 2003/04/29 07:19:30 dustin Exp $

create view temps_per_month as
	select
			date(date_trunc('month', ts)) as month, 
			avg(sample) as temp,
			samples.sensor_id, name
		from
			samples join sensors using (sensor_id)
		group by
			month, samples.sensor_id, name
		order by
			month
;

-- Monthly rollups
create table rollups_month (
	sensor_id integer not null,
	min_reading float not null,
	avg_reading float not null,
	stddev_reading float not null,
	max_reading float not null,
	month date not null,
	foreign key(sensor_id) references sensors(sensor_id)
);
create unique index rollups_mo_sensts on rollups_month(sensor_id, month);

-- Update the monthly rollups
insert into rollups_month
  select
  	sensor_id,
		min(sample) as min_reading,
		avg(sample) as avg_reading,
		stddev(sample) as stddev_reading,
		max(sample) as max_reading,
		date(date_trunc('month', ts)) as month
  from
    samples
  where
  	ts > (select max(month) from rollups_month)
	and ts < date(date_trunc('month', current_date))
  group by
  	sensor_id, month
  having
    date(date_trunc('month', ts)) > (select max(month) from rollups_month)
;

-- daily rollups
create table rollups_day (
	sensor_id integer not null,
	min_reading float not null,
	avg_reading float not null,
	stddev_reading float not null,
	max_reading float not null,
	day date not null,
	foreign key(sensor_id) references sensors(sensor_id)
);
create unique index rollups_d_sensts on rollups_day(sensor_id, day);
create index rollups_day_byts on rollups_day(day);

-- Update the daily rollups
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
  	ts > (select max(day) from rollups_day)
	and ts < current_date
  group by
  	sensor_id, day
;
