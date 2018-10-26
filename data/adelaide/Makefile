# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: adelaide.mdst adelaide.csv

adelaide.mdst: mdst operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

copy: adelaide.copy

clean:
	rm -f adelaide.mdst adelaide.csv

include ../Makefile.common
