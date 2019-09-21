# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: navigo.mdst navigo.csv

navigo.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 \
		-n NOTICE stations.csv

copy: navigo.copy

clean:
	rm -f navigo.mdst navigo.csv

include ../Makefile.common
