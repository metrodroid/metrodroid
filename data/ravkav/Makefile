# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

all: ravkav.mdst ravkav.csv

ravkav.mdst: mapping.csv operators.csv languages.csv ravkav_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py \
		ravkav_gtfs.zip \
		-m mapping.csv \
		-p operators.csv \
		-F languages.csv \
		-n NOTICE \
		-o $@

ravkav_old.mdst: mdst operators.csv stations.csv
	python3 $(MDST)/csv2pb.py -p operators.csv -o $@ -V 2 -n NOTICE \
		stations.csv

copy: ravkav.copy

clean:
	rm -f ravkav.mdst ravkav.csv

include ../Makefile.common
