# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

GTFS_URL = "https://www.transperth.wa.gov.au/TimetablePDFs/GoogleTransit/Production/google_transit.zip"

all: smartrider.mdst smartrider.csv

smartrider.mdst: mapping.csv transperth_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py \
		transperth_gtfs.zip \
		-m mapping.csv \
		-l en-AU \
		-n NOTICE \
		-X "stn" \
		-o $@

transperth_gtfs.zip:
	curl -o $@ -z $@ $(GTFS_URL)

copy: smartrider.copy

clean:
	rm -f transperth_gtfs.zip smartrider.mdst smartrider.csv

include ../Makefile.common

