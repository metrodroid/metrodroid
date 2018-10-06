# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: tfi_leap.mdst tfi_leap.csv

tfi_leap.mdst: agencies.csv mapping.csv google_transit_irishrail.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py google_transit_irishrail.zip -m mapping.csv -l en-IE -L ga,ga-IE -a 21 -p agencies.csv -n NOTICE -o $@

google_transit_irishrail.zip:
	wget -N https://www.transportforireland.ie/transitData/$@

clean:
	rm -f google_transit_irishrail.zip tfi_leap.mdst tfi_leap.csv

include ../Makefile.common

