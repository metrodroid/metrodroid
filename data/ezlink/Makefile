# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

NAMES_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/PublicTransportRelated/Train%20Station%20Codes%20and%20Chinese%20Names.zip"
STATIONS_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/Geospatial/TrainStation.zip"

MDST = ../../extra/mdst

all: ezlink.mdst ezlink.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

clean:
	rm -f ezlink.mdst ezlink.csv

ezlink.mdst: mapping.csv mdst_deps names.zip stations.zip
	python3 $(MDST)/ezlink.py

stations.zip:
	wget -O $@ -N "$(STATIONS_ZIP_URL)"

names.zip:
	wget -O $@ -N "$(NAMES_ZIP_URL)"

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

