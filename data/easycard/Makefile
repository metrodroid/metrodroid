# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

MDST = ../../extra/mdst

all: easycard.mdst easycard.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/csv2pb.py

clean:
	rm -f easycard.mdst easycard.csv

easycard.mdst: mdst_deps stations.csv lines.csv
	python3 $(MDST)/csv2pb.py stations.csv -r lines.csv -L zh-TW -l zh-TW -o $@ -V 3

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

