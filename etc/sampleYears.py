#!/usr/bin/env python
"""
Build a schema for doing partitions kind of stuff on the samples table.

Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
"""

import sys

queryBase="""
create table samples_%(year)s (
	ts timestamp not null,
	sensor_id integer not null,
	sample float not null,
	foreign key(sensor_id) references sensors(sensor_id)
)
;
create unique index samples_%(year)s_bytimeid on samples_%(year)s(ts, sensor_id)
;
"""

# Build the tables
for i in sys.argv[1:]:
    print queryBase % {'year':i}

# Build the view
v="create view samples as\n"
parts=[]
for i in sys.argv[1:]:
    parts.append("\tselect * from samples_%s\n" % i)
v = v + " union ".join(parts) + ";"
print v

print "grant select on samples to nobody;"
print """
create rule sample_rule as on insert to samples
  do instead
  insert into samples_%s (ts, sensor_id, sample)
    values(new.ts, new.sensor_id, new.sample)
;
grant insert on samples to tempload;
""" % (sys.argv[-1])
