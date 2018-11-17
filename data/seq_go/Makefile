# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

SEQ_URL = "https://gtfsrt.api.translink.com.au/GTFS/SEQ_GTFS.zip"

all: seq_go.mdst seq_go.csv

seq_go.mdst: mapping.csv operators.csv seq_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py seq_gtfs.zip -m mapping.csv -p operators.csv -l en-AU -n NOTICE -o $@

seq_gtfs.zip:
	curl -o $@ -z $@ $(SEQ_URL)

copy: seq_go.copy

clean:
	rm -f seq_gtfs.zip seq_go.mdst seq_go.csv

include ../Makefile.common

