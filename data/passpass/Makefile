# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: passpass.mdst passpass.csv

passpass.mdst: mdst operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

copy: passpass.copy

clean:
	rm -f passpass.mdst passpass.csv

include ../Makefile.common
