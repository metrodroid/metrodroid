# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-
MDROOT = ../..

AMIIBO_URL=https://raw.githubusercontent.com/N3evin/AmiiboAPI/master/database/amiibo.json
LICENSE_URL=https://raw.githubusercontent.com/N3evin/AmiiboAPI/master/LICENSE

all: amiibo.mdst amiibo.csv

amiibo.json:
	curl -o $@ -z $@ $(AMIIBO_URL)

LICENSE:
	curl -o $@ -z $@ $(LICENSE_URL)

amiibo.mdst: mdst amiibo.json LICENSE
	python3 $(MDST)/amiibo2pb.py -V 2 -o $@ -n LICENSE amiibo.json 

copy: amiibo.copy

clean:
	rm -f amiibo.mdst amiibo.csv

include ../Makefile.common
