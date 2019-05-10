# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: orca_brt.mdst orca_brt.csv

clean:
	rm -f orca_brt.mdst orca_brt.csv

orca_brt.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l en_US

copy: orca_brt.copy

include ../Makefile.common

