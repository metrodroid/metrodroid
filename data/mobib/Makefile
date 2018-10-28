# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

MOBIB_URL = "https://transitfeeds.com/p/societe-des-transports-intercommunaux-de-bruxelles/527/latest/download"

all: mobib.mdst mobib.csv

mobib.mdst: mapping.csv historical_stations.csv mobib_gtfs.zip mdst
	python3 $(MDST)/compile_stops_from_gtfs.py mobib_gtfs.zip -m mapping.csv -l en-US -p operators.csv -x historical_stations.csv -o $@

# Need to follow redirects. (-L)
mobib_gtfs.zip:
	curl -o $@ -z $@ -L $(MOBIB_URL)

copy: mobib.copy

clean:
	rm -f mobib_gtfs.zip mobib.mdst mobib.csv

include ../Makefile.common
