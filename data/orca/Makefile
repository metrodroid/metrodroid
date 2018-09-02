# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: orca.mdst orca.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f orca.mdst orca.csv

orca.mdst: stations.csv mdst_deps
	python3 $(MDST)/csv2pb.py stations.csv -p agencies.csv -o $@ -V 2 -l en_US

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

