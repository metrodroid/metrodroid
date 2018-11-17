# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: tisseo.mdst tisseo.csv

tisseo.mdst: mdst operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

copy: tisseo.copy

clean:
	rm -f tisseo.mdst tisseo.csv

include ../Makefile.common
