# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: touchngo.mdst touchngo.csv

touchngo.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 stations.csv -L ms,ms-MY -l ms

copy: touchngo.copy

clean:
	rm -f touchngo.mdst touchngo.csv

include ../Makefile.common
