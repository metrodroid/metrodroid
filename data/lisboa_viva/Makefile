# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

PT_URL = "https://navitia.opendatasoft.com/explore/dataset/portugal/files/41d3174d323d59b270683a2b3f1245f2/download/"

all: lisboa_viva.mdst lisboa_viva.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/dump2csv.py

lisboa_viva.mdst: mdst operators.csv stations.csv lines.csv pt_gtfs.zip mapping.csv license-full.txt
	python3 $(MDST)/compile_stops_from_gtfs.py pt_gtfs.zip -r lines.csv -m mapping.csv -n license-full.txt -l pt -L pt -p operators.csv -x stations.csv -o $@

pt_gtfs.zip:
	curl -o $@ -z $@ -L $(PT_URL)

license.txt:
	curl -o $@ -z $@ -L https://opendatacommons.org/files/2018/02/odbl-10.txt

license-full.txt: license-prefix.txt license.txt
	cat $^ > $@

copy: lisboa_viva.copy

clean:
	rm -f lisboa_viva.mdst lisboa_viva.csv

include ../Makefile.common
