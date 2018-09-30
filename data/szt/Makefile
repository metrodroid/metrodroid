# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDST = ../../extra/mdst

all: shenzhen.mdst shenzhen.csv

clean:
	rm -f shenzhen.mdst shenzhen.csv

shenzhen.mdst: stations.csv mdst
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l zh -L zh

include ../Makefile.common

