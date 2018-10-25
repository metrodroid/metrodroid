# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

NAMES_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/PublicTransportRelated/Train%20Station%20Codes%20and%20Chinese%20Names.zip"
STATIONS_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/Geospatial/TrainStation.zip"

all: ezlink.mdst ezlink.csv

clean:
	rm -f ezlink.mdst ezlink.csv

ezlink.mdst: mapping.csv mdst names.zip stations.zip NOTICE
	python3 $(MDST)/ezlink.py

stations.zip:
	curl -o $@ -z $@ "$(STATIONS_ZIP_URL)"

names.zip:
	curl -o $@ -z $@ "$(NAMES_ZIP_URL)"

copy: ezlink.copy

include ../Makefile.common

