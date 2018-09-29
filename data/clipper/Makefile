# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst
ZIPFILE = google_transit_20180526_v2.zip
ZIPFILE_VTA = 069A0000001NUea

all: clipper.mdst clipper.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

clipper.mdst: mapping_bart.csv $(ZIPFILE) $(ZIPFILE_VTA) mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py $(ZIPFILE) $(ZIPFILE_VTA) -m mapping_bart.csv -m mapping_vta.csv -l en-US -p operators.csv -x stations.csv -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

$(ZIPFILE):
	wget -N https://www.bart.gov/sites/default/files/docs/$@

$(ZIPFILE_VTA):
	wget -N http://www.vta.org/sfc/servlet.shepherd/document/download/$@

clean:
	rm -f $(ZIPFILE) $(ZIPFILE_VTA) clipper.mdst clipper.csv

