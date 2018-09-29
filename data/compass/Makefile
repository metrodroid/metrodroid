# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: compass.mdst compass.csv

clean:
	rm -f compass.mdst compass.csv

compass.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l en_CA

include ../Makefile.common

