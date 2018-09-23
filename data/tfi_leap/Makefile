# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst

all: tfi_leap.mdst tfi_leap.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

tfi_leap.mdst: mapping.csv google_transit_irishrail.zip mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py google_transit_irishrail.zip -m mapping.csv -l en-IE -L ga,ga-IE -a 21 -p agencies.csv -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

google_transit_irishrail.zip:
	wget -N https://www.transportforireland.ie/transitData/$@

clean:
	rm -f google_transit_irishrail.zip tfi_leap.mdst tfi_leap.csv
