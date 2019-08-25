# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: waltti_region.mdst waltti_region.csv

clean:
	rm -f waltti_region.mdst waltti_region.csv

waltti_region.mdst: regions.csv mdst
	python3 $(MDST)/csv2pb.py empty.csv -p regions.csv -o $@ -V 2 -l zh -L zh

copy: waltti_region.copy

include ../Makefile.common

