# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: orca.mdst orca.csv

clean:
	rm -f orca.mdst orca.csv

orca.mdst: stations.csv agencies.csv lines.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -n NOTICE -p agencies.csv -r lines.csv -o $@ -V 2 -l en_US

copy: orca.copy

include ../Makefile.common

