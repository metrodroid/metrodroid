# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: gautrain.mdst gautrain.csv

gautrain.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 stations.csv

copy: gautrain.copy

clean:
	rm -f gautrain.mdst gautrain.csv

include ../Makefile.common
