# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..
GTFS = $(MDROOT)/third_party/gtfs_lacmta_rail/gtfs_rail.zip

all: lax_tap.mdst lax_tap.csv

lax_tap.mdst: mapping.csv $(GTFS) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py $(GTFS) -m mapping.csv -p operators.csv -l en-US -n NOTICE -o $@

copy: lax_tap.copy

clean:
	rm -f lax_tap.mdst lax_tap.csv

include ../Makefile.common
