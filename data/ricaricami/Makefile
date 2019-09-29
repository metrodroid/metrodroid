# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

AMAT_URL = "https://www.amat-mi.it/it/downloads/data/gtfs/current/Export_OpenDataTPL_Current.zip"

all: ricaricami.mdst ricaricami.csv

ricaricami.mdst: mapping.csv amat_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py amat_gtfs.zip -x stations.csv -m mapping.csv -l it-IT -o $@

amat_gtfs.zip:
	curl -o $@ -z $@ $(AMAT_URL)

copy: ricaricami.copy

clean:
	rm -f amat_gtfs.zip ricaricami.mdst ricaricami.csv

include ../Makefile.common
