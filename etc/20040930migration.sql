begin transaction;
lock table samples;
alter table samples rename to samples_old;

create table samples_1999 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_1999_bytime on samples_1999(ts);
create index samples_1999_byid on samples_1999(sensor_id);


create table samples_2000 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_2000_bytime on samples_2000(ts);
create index samples_2000_byid on samples_2000(sensor_id);


create table samples_2001 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_2001_bytime on samples_2001(ts);
create index samples_2001_byid on samples_2001(sensor_id);


create table samples_2002 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_2002_bytime on samples_2002(ts);
create index samples_2002_byid on samples_2002(sensor_id);


create table samples_2003 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_2003_bytime on samples_2003(ts);
create index samples_2003_byid on samples_2003(sensor_id);


create table samples_2004 (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create index samples_2004_bytime on samples_2004(ts);
create index samples_2004_byid on samples_2004(sensor_id);

create view samples as
	select * from samples_1999
 union 	select * from samples_2000
 union 	select * from samples_2001
 union 	select * from samples_2002
 union 	select * from samples_2003
 union 	select * from samples_2004
;
grant select on samples to nobody;

create rule sample_rule as on insert to samples
  do instead
  insert into samples_2004 (ts, sensor_id, sample)
    values(new.ts, new.sensor_id, new.sample)
;
grant select on samples to nobody;
grant insert on samples to tempload; 

commit;

-- Migration

-- Drop the indexes before proceeding.
drop index samples_1999_bytime;
drop index samples_1999_byid;
drop index samples_2000_bytime;
drop index samples_2000_byid;
drop index samples_2001_bytime;
drop index samples_2001_byid;
drop index samples_2002_bytime;
drop index samples_2002_byid;
drop index samples_2003_bytime;
drop index samples_2003_byid;
drop index samples_2004_bytime;
drop index samples_2004_byid;

begin transaction;
insert into samples_2004
	select ts, sensor_id, sample
		from samples_old
		where ts >= '2004/01/01'::timestamp
;
delete from samples_old where ts >= '2004/01/01'::timestamp
;
commit;
-- Special unique index for inserts.

vacuum verbose samples_old;

create unique index samples_2004_bytimeid on samples_2004(ts, sensor_id);

begin transaction;
insert into samples_2003
	select ts, sensor_id, sample
		from samples_old
		where ts >= '2003/01/01'::timestamp
;
delete from samples_old where ts >= '2003/01/01'::timestamp
;
commit;

vacuum verbose samples_old;

create unique index samples_2003_bytimeid on samples_2003(ts, sensor_id);

begin transaction;
insert into samples_2002
	select ts, sensor_id, sample
		from samples_old
		where ts >= '2002/01/01'::timestamp
;
delete from samples_old where ts >= '2002/01/01'::timestamp
;
commit;

vacuum verbose samples_old;

create unique index samples_2002_bytimeid on samples_2002(ts, sensor_id);

begin transaction;
insert into samples_2001
	select ts, sensor_id, sample
		from samples_old
		where ts >= '2001/01/01'::timestamp
;
delete from samples_old where ts >= '2001/01/01'::timestamp
;
commit;

vacuum verbose samples_old;

create unique index samples_2001_bytimeid on samples_2001(ts, sensor_id);

begin transaction;
insert into samples_2000
	select ts, sensor_id, sample
		from samples_old
		where ts >= '2000/01/01'::timestamp
;
delete from samples_old where ts >= '2000/01/01'::timestamp
;
commit;

vacuum verbose samples_old;

create unique index samples_2000_bytimeid on samples_2000(ts, sensor_id);

begin transaction;
insert into samples_1999
	select ts, sensor_id, sample
		from samples_old
		where ts >= '1999/01/01'::timestamp
;
delete from samples_old where ts >= '1999/01/01'::timestamp
;
commit;

vacuum verbose samples_old;

create unique index samples_1999_bytimeid on samples_1999(ts, sensor_id);
