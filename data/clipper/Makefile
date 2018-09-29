# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst
ZIPFILE = google_transit_20180526_v2.zip
MAPPING_FILES = mapping_bart.csv operators.csv stations.csv

all: clipper.mdst clipper.csv

clipper.mdst: $(MAPPING_FILES) $(ZIPFILE) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py $(ZIPFILE) -m mapping_bart.csv -l en-US -p operators.csv -x stations.csv -o $@

$(ZIPFILE):
	wget -N https://www.bart.gov/sites/default/files/docs/$@

clean:
	rm -f $(ZIPFILE) clipper.mdst clipper.csv

include ../Makefile.common

