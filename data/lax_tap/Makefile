# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst
GTFS = ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip

all: lax_tap.mdst lax_tap.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

lax_tap.mdst: mapping.csv $(GTFS) mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py $(GTFS) -m mapping.csv -p operators.csv -l en-US -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

clean:
	rm -f lax_tap.mdst lax_tap.csv

