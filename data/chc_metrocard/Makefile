# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: chc_metrocard.mdst chc_metrocard.csv

chc_metrocard.mdst: mdst operators.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 -l en-NZ -L mi,mi-NZ stations.csv

copy: chc_metrocard.copy

clean:
	rm -f chc_metrocard.mdst chc_metrocard.csv

include ../Makefile.common

