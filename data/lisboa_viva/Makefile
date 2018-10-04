# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst

all: lisboa_viva.mdst lisboa_viva.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/dump2csv.py

lisboa_viva.mdst: mdst_deps operators.csv stations.csv lines.csv
	python3 $(MDST)/csv2pb.py -r lines.csv -p operators.csv -o $@ -V 2 stations.csv

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

clean:
	rm -f lisboa_viva.mdst lisboa_viva.csv
