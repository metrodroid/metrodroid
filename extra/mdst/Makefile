# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

all: stations_pb2.py suica_rail.mdst suica_bus.mdst opus.mdst shenzhen.mdst ovc.mdst easycard.mdst orca.mdst compass.mdst podorozhnik.mdst troika.mdst suica_rail.csv suica_bus.csv ovc.csv opus.csv shenzhen.csv easycard.csv orca.csv compass.csv podorozhnik.csv troika.csv

stations_pb2.py: stations.proto
	protoc --python_out=. stations.proto

clean:
	rm -f stations_pb2.py
	rm -f suica_rail.mdst suica_rail.csv
	rm -f suica_bus.mdst suica_bus.csv
	rm -f ovc.mdst ovc.csv

suica_rail.mdst: sqlite2pb-suica.py stations_pb2.py mdst.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica.py

suica_bus.mdst: sqlite2pb-suica-bus.py stations_pb2.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica-bus.py

ovc.mdst: sqlite2pb-ovc.py stations_pb2.py mdst.py ../../data/ovc_stations.db3
	python3 sqlite2pb-ovc.py

shenzhen.mdst: csv2pb-szt.py stations_pb2.py mdst.py ../../data/szt/*.txt
	python3 csv2pb-szt.py

podorozhnik.mdst: csv2pb-podorozhnik.py stations_pb2.py mdst.py ../../data/podorozhnik/*.csv
	python3 csv2pb-podorozhnik.py podorozhnik

troika.mdst: csv2pb-podorozhnik.py stations_pb2.py mdst.py ../../data/troika/*.csv
	python3 csv2pb-podorozhnik.py troika

ezlink.mdst: csv2pb-ezlink.py stations_pb2.py mdst.py ../../data/ezlink/*.csv
	python3 csv2pb-ezlink.py

%.csv: %.mdst dump2csv.py stations_pb2.py
	python3 dump2csv.py -o $@ $<

