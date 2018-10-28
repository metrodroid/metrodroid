# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: ravkav.mdst ravkav.csv

ravkav.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 stations.csv

copy: ravkav.copy

clean:
	rm -f ravkav.mdst ravkav.csv

include ../Makefile.common
