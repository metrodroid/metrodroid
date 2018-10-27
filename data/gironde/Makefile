# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: gironde.mdst gironde.csv

gironde.mdst: mdst operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

copy: gironde.copy

clean:
	rm -f gironde.mdst gironde.csv

include ../Makefile.common
