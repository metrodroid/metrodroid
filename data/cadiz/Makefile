# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: cadiz.mdst cadiz.csv

clean:
	rm -f cadiz.mdst cadiz.csv

cadiz.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -r lines.csv -o $@ -V 2 -l es_SP 

copy: cadiz.copy

include ../Makefile.common

