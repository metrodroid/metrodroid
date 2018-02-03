# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

all: stations_pb2.py suica_rail.mdst suica_bus.mdst ovc.mdst suica_rail.csv suica_bus.csv ovc.csv

stations_pb2.py: stations.proto
	protoc --python_out=. stations.proto

clean:
	rm -f stations_pb2.py suica_rail.mdst suica_bus.mdst

suica_rail.mdst: sqlite2pb-suica.py stations_pb2.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica.py

suica_rail.csv: dump2csv.py stations_pb2.py suica_rail.mdst
	python3 dump2csv.py -o suica_rail.csv suica_rail.mdst

suica_bus.mdst: sqlite2pb-suica-bus.py stations_pb2.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica-bus.py

suica_bus.csv: dump2csv.py stations_pb2.py suica_bus.mdst
	python3 dump2csv.py -o suica_bus.csv suica_bus.mdst

ovc.mdst: sqlite2pb-ovc.py stations_pb2.py ../../data/ovc_stations.db3
	python3 sqlite2pb-ovc.py

ovc.csv: dump2csv.py stations_pb2.py ovc.mdst
	python3 dump2csv.py -o ovc.csv ovc.mdst

