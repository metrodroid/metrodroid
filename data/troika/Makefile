# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: troika.mdst troika.csv

clean:
	rm -f troika.mdst troika.csv

troika.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l ru -L ru

copy: troika.copy

include ../Makefile.common
