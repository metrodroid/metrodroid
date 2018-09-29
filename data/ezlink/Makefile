# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
NAMES_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/PublicTransportRelated/Train%20Station%20Codes%20and%20Chinese%20Names.zip"
STATIONS_ZIP_URL = "https://www.mytransport.sg/content/dam/datamall/datasets/Geospatial/TrainStation.zip"

MDST = ../../extra/mdst

all: ezlink.mdst ezlink.csv

clean:
	rm -f ezlink.mdst ezlink.csv

ezlink.mdst: mapping.csv mdst names.zip stations.zip
	python3 $(MDST)/ezlink.py

stations.zip:
	wget -O $@ -N "$(STATIONS_ZIP_URL)"

names.zip:
	wget -O $@ -N "$(NAMES_ZIP_URL)"

include ../Makefile.common

