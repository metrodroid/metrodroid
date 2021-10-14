# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: yargor.mdst yargor.csv

yargor.mdst: mdst stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -l ru -L ru -r lines.csv -o $@ -V 2 stations.csv

copy: yargor.copy

clean:
	rm -f yargor.mdst yargor.csv

include ../Makefile.common
