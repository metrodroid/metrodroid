# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: kmt.mdst kmt.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f kmt.mdst kmt.csv

kmt.mdst: stations.csv mdst_deps
	python3 $(MDST)/csv2pb.py ../../data/kmt/stations.csv -o $@ -V 2 -l id -L id

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

