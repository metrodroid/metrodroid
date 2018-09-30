# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: orca.mdst orca.csv

clean:
	rm -f orca.mdst orca.csv

orca.mdst: stations.csv agencies.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -p agencies.csv -o $@ -V 2 -l en_US

include ../Makefile.common

