# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst
ZIPFILE = google_transit_20180526_v2.zip

all: clipper.mdst clipper.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

clipper.mdst: mapping_bart.csv $(ZIPFILE) mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py $(ZIPFILE) -m mapping_bart.csv -l en-US -p operators.csv -x stations.csv -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

$(ZIPFILE):
	wget -N https://www.bart.gov/sites/default/files/docs/$@

clean:
	rm -f $(ZIPFILE) clipper.mdst clipper.csv

