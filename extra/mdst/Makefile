# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

all: stations_pb2.py suica_rail.mdst suica_bus.mdst shenzhen.mdst ovc.mdst kmt.mdst orca.mdst compass.mdst troika.mdst podorozhnik.mdst suica_rail.csv suica_bus.csv ovc.csv shenzhen.csv orca.csv compass.csv kmt.csv troika.csv orca.csv podorozhnik.csv

stations_pb2.py: stations.proto
	protoc --python_out=. stations.proto

clean:
	rm -f stations_pb2.py
	rm -f suica_rail.mdst suica_rail.csv
	rm -f suica_bus.mdst suica_bus.csv
	rm -f ovc.mdst ovc.csv

orca.mdst: csv2pb-orca.py stations_pb2.py mdst.py ../../data/orca/*.csv
	python3 csv2pb-orca.py ../../data/orca orca.mdst

suica_rail.mdst: sqlite2pb-suica.py stations_pb2.py mdst.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica.py

suica_bus.mdst: sqlite2pb-suica-bus.py stations_pb2.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica-bus.py

kmt.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/kmt/stations.csv
	python3 csv2pb.py ../../data/kmt/stations.csv -o $@ -V 2 -L id

ovc.mdst: sqlite2pb-ovc.py stations_pb2.py mdst.py ../../data/ovc_stations.db3
	python3 sqlite2pb-ovc.py

shenzhen.mdst: csv2pb-szt.py stations_pb2.py mdst.py ../../data/szt/*.txt
	python3 csv2pb-szt.py

podorozhnik.mdst: csv2pb-podorozhnik.py stations_pb2.py mdst.py ../../data/podorozhnik/*.csv
	python3 csv2pb-podorozhnik.py

compass.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/compass/stations.csv
	python3 csv2pb.py ../../data/compass/stations.csv -o $@ -V 2

troika.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/troika/stations.csv
	python3 csv2pb.py ../../data/troika/stations.csv -o $@ -V 2 -L ru

%.csv: %.mdst dump2csv.py stations_pb2.py
	python3 dump2csv.py -o $@ $<

