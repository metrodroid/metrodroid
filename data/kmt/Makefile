# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: kmt.mdst kmt.csv

clean:
	rm -f kmt.mdst kmt.csv

kmt.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l id -L id

copy: kmt.copy

include ../Makefile.common
