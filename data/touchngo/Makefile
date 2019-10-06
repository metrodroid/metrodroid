# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: touchngo.mdst touchngo.csv

touchngo.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -o $@ -V 2 \
		-L ms,ms-MY -l ms \
		-p operators.csv \
		-r lines.csv \
		stations.csv

copy: touchngo.copy

clean:
	rm -f touchngo.mdst touchngo.csv

include ../Makefile.common
