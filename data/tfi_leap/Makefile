# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

IRISHRAIL_URL = "https://www.transportforireland.ie/transitData/google_transit_irishrail.zip"

all: tfi_leap.mdst tfi_leap.csv

tfi_leap.mdst: agencies.csv mapping.csv irishrail_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py irishrail_gtfs.zip -m mapping.csv -l en-IE -L ga,ga-IE -a 21 -p agencies.csv -n NOTICE -o $@

irishrail_gtfs.zip:
	curl -o $@ -z $@ $(IRISHRAIL_URL)

copy: tfi_leap.copy

clean:
	rm -f google_transit_irishrail.zip tfi_leap.mdst tfi_leap.csv

include ../Makefile.common

