# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: compass.mdst compass.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f compass.mdst compass.csv

compass.mdst: stations.csv mdst_deps
	python3 $(MDST)/csv2pb.py ../../data/compass/stations.csv -o $@ -V 2 -l en_CA

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

