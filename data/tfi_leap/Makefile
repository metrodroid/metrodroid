# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

DUBLINBUS_URL = "https://www.transportforireland.ie/transitData/Data/GTFS_Dublin_Bus.zip"
IRISHRAIL_URL = "https://www.transportforireland.ie/transitData/Data/GTFS_Irish_Rail.zip"
LUAS_URL = "https://www.transportforireland.ie/transitData/Data/GTFS_LUAS.zip"

MAPPING_FILES := \
    mapping_dublinbus.csv \
    mapping_irishrail.csv \
    mapping_luas.csv

GTFS_FILES := \
    dublinbus_gtfs.zip \
    irishrail_gtfs.zip \
    luas_gtfs.zip

all: tfi_leap.mdst tfi_leap.csv

tfi_leap.mdst: agencies.csv $(MAPPING_FILES) $(GTFS_FILES) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py \
	    dublinbus_gtfs.zip \
	    irishrail_gtfs.zip \
	    luas_gtfs.zip \
	    -m mapping_dublinbus.csv \
	    -a 20 \
	    -m mapping_irishrail.csv \
	    -a 21 \
	    -m mapping_luas.csv \
	    -a 23 \
	    -X 'luas stop' \
	    -p agencies.csv -n NOTICE -l en-IE -L ga,ga-IE -o $@

dublinbus_gtfs.zip:
	curl -o $@ -z $@ $(DUBLINBUS_URL)

irishrail_gtfs.zip:
	curl -o $@ -z $@ $(IRISHRAIL_URL)

luas_gtfs.zip:
	curl -o $@ -z $@ $(LUAS_URL)

copy: tfi_leap.copy

clean:
	rm -f tfi_leap.mdst tfi_leap.csv \
	    $(GTFS_FILES)

include ../Makefile.common
