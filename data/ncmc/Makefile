# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: ncmc.mdst ncmc.csv

clean:
	rm -f ncmc.mdst ncmc.csv

ncmc.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 -l en-IN stations.csv

copy: ncmc.copy

include ../Makefile.common
