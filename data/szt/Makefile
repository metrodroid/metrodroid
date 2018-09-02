# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: shenzhen.mdst shenzhen.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f shenzhen.mdst shenzhen.csv

shenzhen.mdst: stations.csv mdst_deps
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l zh -L zh

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

