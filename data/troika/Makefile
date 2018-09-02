# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: troika.mdst troika.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f troika.mdst troika.csv

troika.mdst: stations.csv mdst_deps
	python3 $(MDST)/csv2pb.py stations.csv -o $@ -V 2 -l ru -L ru

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

