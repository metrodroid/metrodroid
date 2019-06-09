# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: hafilat.mdst hafilat.csv

hafilat.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 stations.csv

copy: hafilat.copy

clean:
	rm -f hafilat.mdst hafilat.csv

include ../Makefile.common
