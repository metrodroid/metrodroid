# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst
GTFS_FILE = Export_OpenDataTPL_Current.zip

all: ricaricami.mdst ricaricami.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

ricaricami.mdst: mapping.csv $(GTFS_FILE) mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py $(GTFS_FILE) -m mapping.csv -l it-IT -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

$(GTFS_FILE):
	wget -N http://amat-mi.it/it/downloads/data/gtfs/current/$@

clean:
	rm -f $(GTFS_FILE) ricaricami.mdst ricaricami.csv
