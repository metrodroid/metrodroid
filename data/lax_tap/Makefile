# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst
GTFS = ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip

all: lax_tap.mdst lax_tap.csv

lax_tap.mdst: mapping.csv $(GTFS) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py $(GTFS) -m mapping.csv -p operators.csv -l en-US -o $@

clean:
	rm -f lax_tap.mdst lax_tap.csv

include ../Makefile.common

