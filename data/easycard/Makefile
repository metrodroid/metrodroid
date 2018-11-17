# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: easycard.mdst easycard.csv

clean:
	rm -f easycard.mdst easycard.csv

easycard.mdst: mdst stations.csv lines.csv
	python3 $(MDST)/csv2pb.py stations.csv -r lines.csv -L zh-TW,zh -l zh-TW -o $@ -V 3

copy: easycard.copy

include ../Makefile.common
