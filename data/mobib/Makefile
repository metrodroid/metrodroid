# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
.PHONY: clean mdst_deps

MDST = ../../extra/mdst
ZIPFILE = download

all: mobib.mdst mobib.csv

mdst_deps: $(MDST)/stations_pb2.py $(MDST)/mdst.py $(MDST)/compile_stops_from_gtfs.py

mobib.mdst: mapping.csv historical_stations.csv $(ZIPFILE) mdst_deps
	python3 $(MDST)/compile_stops_from_gtfs.py $(ZIPFILE) -m mapping.csv -l en-US -p operators.csv -x historical_stations.csv -o $@

%.csv: %.mdst mdst_deps
	python3 $(MDST)/dump2csv.py -o $@ $<

$(ZIPFILE):
	wget -N https://transitfeeds.com/p/societe-des-transports-intercommunaux-de-bruxelles/527/latest/$@

clean:
	rm -f $(ZIPFILE) mobib.mdst mobib.csv

