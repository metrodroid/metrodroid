# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

ZIPFILE_BART = google_transit_20180526_v2.zip
ZIPFILE_VTA = 069A0000001NUea
ZIP_FILES := $(ZIPFILE_BART) $(ZIPFILE_VTA)

MAPPING_FILES = mapping_bart.csv mapping_vta.csv operators.csv stations.csv

all: clipper.mdst clipper.csv

clipper.mdst: $(MAPPING_FILES) $(ZIP_FILES) mdst
	python3 $(MDST)/compile_stops_from_gtfs.py $(ZIPFILE_BART) $(ZIPFILE_VTA) -m mapping_bart.csv -m mapping_vta.csv -l en-US -p operators.csv -x stations.csv -o $@

$(ZIPFILE_BART):
	wget -N https://www.bart.gov/sites/default/files/docs/$@

$(ZIPFILE_VTA):
	wget -N http://www.vta.org/sfc/servlet.shepherd/document/download/$@

clean:
	rm -f $(ZIPFILE_BART) $(ZIPFILE_VTA) clipper.mdst clipper.csv

include ../Makefile.common

