# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

IRISHRAIL_URL = "https://www.transportforireland.ie/transitData/google_transit_irishrail.zip"
LUAS_URL = "https://www.transportforireland.ie/transitData/google_transit_luas.zip"

MAPPING_FILES := \
    mapping_irishrail.csv \
    mapping_luas.csv

GTFS_FILES := \
    irishrail_gtfs.zip \
    luas_gtfs.zip

all: tfi_leap.mdst tfi_leap.csv

tfi_leap.mdst: agencies.csv $(MAPPING_FILES) $(GTFS_FILES) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py \
	    irishrail_gtfs.zip \
	    luas_gtfs.zip \
	    -m mapping_irishrail.csv \
	    -a 21 \
	    -m mapping_luas.csv \
	    -a 23 \
	    -X 'luas stop' \
	    -p agencies.csv -n NOTICE -l en-IE -L ga,ga-IE -o $@

irishrail_gtfs.zip:
	curl -o $@ -z $@ $(IRISHRAIL_URL)

luas_gtfs.zip:
	curl -o $@ -z $@ $(LUAS_URL)

copy: tfi_leap.copy

clean:
	rm -f tfi_leap.mdst tfi_leap.csv \
	    $(GTFS_FILES)

include ../Makefile.common
