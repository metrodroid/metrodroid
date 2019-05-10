# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: orca_streetcar.mdst orca_streetcar.csv

clean:
	rm -f orca_streetcar.mdst orca_streetcar.csv

orca_streetcar.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l en_US

copy: orca_streetcar.copy

include ../Makefile.common

