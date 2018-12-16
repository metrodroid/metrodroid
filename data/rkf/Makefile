# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: rkf.mdst rkf.csv

rkf.mdst: mdst operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

copy: rkf.copy

clean:
	rm -f rkf.mdst rkf.csv

include ../Makefile.common
