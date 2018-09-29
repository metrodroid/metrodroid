# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: troika.mdst troika.csv

clean:
	rm -f troika.mdst troika.csv

troika.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l ru -L ru

include ../Makefile.common

