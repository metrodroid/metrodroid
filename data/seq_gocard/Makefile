# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst

all: seq_go.mdst seq_go.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

seq_go.mdst: mapping.csv SEQ_GTFS.zip mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py SEQ_GTFS.zip -m mapping.csv -l en-AU -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

SEQ_GTFS.zip:
	wget -N https://gtfsrt.api.translink.com.au/GTFS/$@

clean:
	rm -f SEQ_GTFS.zip seq_go.mdst seq_go.csv

