# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

BART_URL = https://www.bart.gov/dev/schedules/google_transit.zip
VTA_URL = https://gtfs.vta.org/gtfs_vta.zip
MUNI_URL = http://gtfs.sfmta.com/transitdata/google_transit.zip
ZIP_FILES := bart_gtfs.zip vta_gtfs.zip muni_gtfs.zip

MAPPING_FILES = mapping_bart.csv mapping_vta.csv mapping_muni.csv operators.csv stations.csv

all: clipper.mdst clipper.csv

clipper.mdst: $(MAPPING_FILES) $(ZIP_FILES) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py \
	    bart_gtfs.zip \
	    vta_gtfs.zip \
	    muni_gtfs.zip \
	    -m mapping_bart.csv \
	    -m mapping_vta.csv \
	    -m mapping_muni.csv \
	    -l en-US \
	    -p operators.csv \
	    -x stations.csv \
	    -n NOTICE \
	    -o $@

bart_gtfs.zip:
	curl -L -o $@ -z $@ $(BART_URL)

vta_gtfs.zip:
	curl -L -o $@ -z $@ $(VTA_URL)

muni_gtfs.zip:
	curl -L -o $@ -z $@ $(MUNI_URL)

copy: clipper.copy

clean:
	rm -f $(ZIPFILE_BART) $(ZIPFILE_VTA) $(ZIPFILE_MUNI) clipper.mdst clipper.csv

include ../Makefile.common

