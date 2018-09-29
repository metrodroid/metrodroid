# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: seq_go.mdst seq_go.csv

seq_go.mdst: mapping.csv operators.csv SEQ_GTFS.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py SEQ_GTFS.zip -m mapping.csv -p operators.csv -l en-AU -o $@

SEQ_GTFS.zip:
	wget -N https://gtfsrt.api.translink.com.au/GTFS/$@

clean:
	rm -f SEQ_GTFS.zip seq_go.mdst seq_go.csv

include ../Makefile.common

