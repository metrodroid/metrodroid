# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

BART_URL = https://www.bart.gov/sites/default/files/docs/google_transit_20180526_v2.zip
VTA_URL = http://www.vta.org/sfc/servlet.shepherd/document/download/069A0000001NUea
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
	curl -o $@ -z $@ $(BART_URL)

# VTA's server doesn't handle timestamps correctly, so this always tries to overwrite.
vta_gtfs.zip:
	curl -o $@ -z $@ $(VTA_URL)

muni_gtfs.zip:
	curl -o $@ -z $@ $(MUNI_URL)

copy: clipper.copy

clean:
	rm -f $(ZIPFILE_BART) $(ZIPFILE_VTA) $(ZIPFILE_MUNI) clipper.mdst clipper.csv

include ../Makefile.common

